package dev.seedxray.core;

import dev.seedxray.SeedXray;
import dev.seedxray.config.SxConfig;
import dev.seedxray.sim.Dim;
import dev.seedxray.sim.OreChunk;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Client-thread side of the ESP: decides what the background {@link Predictor} should compute, scans loaded chunks
 * for the selected blocks, and merges everything into the {@link BoxList} the renderer draws.
 */
public final class EspController {
	private static final int SCAN_PER_TICK = 2;
	private static final int RESCAN_TICKS = 600;
	private static final int PER_CHUNK_LIMIT = 1500;
	/** Stop scanning once this many blocks are remembered (someone selected a very common block). */
	private static final int TOTAL_LIMIT = 60_000;

	private record Scan(int tick, int version, long[] positions, Block[] blocks) {
	}

	private final Long2ObjectOpenHashMap<Scan> scans = new Long2ObjectOpenHashMap<>();
	private volatile BoxList boxes = BoxList.EMPTY;

	private int ticks;
	private Dim lastDim;
	private long lastSeed;
	private boolean lastSeedKnown;
	private int lastSelection = -1;
	private BlockPos lastStructCenter;
	private int lastStructRadius;
	private int lastOreCx = Integer.MIN_VALUE, lastOreCz;
	private int lastOreRadius;
	private int lastBuildTick = -100;
	private BlockPos lastBuildPos = BlockPos.ORIGIN;

	private Map<Block, Integer> selectedBlocks = Map.of();
	private int selectedBlocksVersion = -1;
	private int scanVersion;

	public BoxList boxes() {
		return boxes;
	}

	public void clear() {
		scans.clear();
		boxes = BoxList.EMPTY;
		lastStructCenter = null;
		lastOreCx = Integer.MIN_VALUE;
		lastSelection = -1;
		lastBuildTick = -100;
	}

	public void tick(MinecraftClient client) {
		ClientWorld world = client.world;
		if (world == null || client.player == null) {
			if (boxes != BoxList.EMPTY) clear();
			return;
		}
		SxConfig cfg = SeedXray.config;
		ticks++;
		Dim dim = Dim.of(world.getRegistryKey());
		SeedXray.dim = dim;
		boolean seedKnown = SeedXray.seed.known();
		long seed = SeedXray.seed.seed;
		if (dim != lastDim || seedKnown != lastSeedKnown || seed != lastSeed) {
			lastDim = dim;
			lastSeedKnown = seedKnown;
			lastSeed = seed;
			lastStructCenter = null;
			lastOreCx = Integer.MIN_VALUE;
			lastBuildTick = -100;
			scans.clear();
		}
		if (!cfg.enabled) {
			if (boxes != BoxList.EMPTY) boxes = BoxList.EMPTY;
			return;
		}
		BlockPos pos = client.player.getBlockPos();
		int selection = SeedXray.selectionVersion;
		boolean selectionChanged = selection != lastSelection;
		lastSelection = selection;

		// --- structures (background)
		if (seedKnown && cfg.structureEsp && !cfg.structures.isEmpty()) {
			int radius = dim == Dim.END ? Math.max(cfg.structureRadius, 160) : cfg.structureRadius;
			if (selectionChanged || lastStructCenter == null || radius != lastStructRadius
				|| lastStructCenter.getSquaredDistance(pos) > 48 * 48) {
				lastStructCenter = pos;
				lastStructRadius = radius;
				SeedXray.predictor.requestStructures(seed, dim, pos, radius, cfg.structures);
			}
		}

		// --- predicted ores (background)
		boolean wantOres = seedKnown && cfg.blockEsp && cfg.predictOres && !cfg.blocks.isEmpty() && dim != Dim.END;
		if (wantOres) {
			int cx = pos.getX() >> 4;
			int cz = pos.getZ() >> 4;
			if (cx != lastOreCx || cz != lastOreCz || cfg.oreRadius != lastOreRadius) {
				lastOreCx = cx;
				lastOreCz = cz;
				lastOreRadius = cfg.oreRadius;
				SeedXray.predictor.requestOres(seed, dim, cx, cz, cfg.oreRadius);
			}
		}

		// --- boxes
		if (!cfg.blockEsp || cfg.blocks.isEmpty()) {
			if (boxes != BoxList.EMPTY) boxes = BoxList.EMPTY;
			return;
		}
		if (selectedBlocksVersion != selection) {
			selectedBlocks = resolveSelectedBlocks(cfg);
			selectedBlocksVersion = selection;
			scanVersion++;
			lastBuildTick = -100;
		}
		if (cfg.scanLoadedChunks) scanLoaded(client, world, pos);
		boolean moved = lastBuildPos.getSquaredDistance(pos) > 9;
		if (ticks - lastBuildTick >= (cfg.lowEnd ? 20 : 8) || (moved && ticks - lastBuildTick >= 4)) {
			lastBuildTick = ticks;
			lastBuildPos = pos;
			boxes = build(world, pos, cfg, wantOres);
		}
	}

	private static Map<Block, Integer> resolveSelectedBlocks(SxConfig cfg) {
		Map<Block, Integer> map = new HashMap<>();
		for (String id : cfg.blocks) {
			Identifier identifier = Identifier.tryParse(id);
			if (identifier == null) continue;
			Block block = Registries.BLOCK.get(identifier);
			if (Registries.BLOCK.getId(block).equals(identifier)) map.put(block, Colors.block(block));
		}
		return map;
	}

	// ---------------------------------------------------------------- loaded chunks

	private void scanLoaded(MinecraftClient client, ClientWorld world, BlockPos player) {
		int view = Math.min(client.options.getClampedViewDistance(), 16);
		int pcx = player.getX() >> 4;
		int pcz = player.getZ() >> 4;
		if (ticks % 20 == 0) {
			scans.keySet().removeIf(key -> {
				ChunkPos p = new ChunkPos(key);
				return Math.abs(p.x - pcx) > view + 2 || Math.abs(p.z - pcz) > view + 2;
			});
		}
		int budget = SCAN_PER_TICK;
		int remembered = 0;
		for (Scan s : scans.values()) remembered += s.positions.length;
		if (remembered > TOTAL_LIMIT) return;
		for (int ring = 0; ring <= view && budget > 0; ring++) {
			for (int dx = -ring; dx <= ring && budget > 0; dx++) {
				for (int dz = -ring; dz <= ring && budget > 0; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
					long key = ChunkPos.toLong(pcx + dx, pcz + dz);
					Scan scan = scans.get(key);
					if (scan != null && scan.version == scanVersion && ticks - scan.tick < RESCAN_TICKS) continue;
					WorldChunk chunk = world.getChunkManager().getWorldChunk(pcx + dx, pcz + dz);
					if (chunk == null) continue;
					scans.put(key, scanChunk(chunk));
					budget--;
				}
			}
		}
	}

	private Scan scanChunk(WorldChunk chunk) {
		Map<Block, Integer> wanted = selectedBlocks;
		long[] out = new long[64];
		Block[] outBlocks = new Block[64];
		int n = 0;
		int bottom = chunk.getBottomSectionCoord();
		ChunkSection[] sections = chunk.getSectionArray();
		ChunkPos cp = chunk.getPos();
		for (int i = 0; i < sections.length && n < PER_CHUNK_LIMIT; i++) {
			ChunkSection section = sections[i];
			if (section == null || section.isEmpty() || !section.hasAny(state -> wanted.containsKey(state.getBlock()))) continue;
			int baseY = (bottom + i) << 4;
			for (int y = 0; y < 16 && n < PER_CHUNK_LIMIT; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						Block block = section.getBlockState(x, y, z).getBlock();
						if (wanted.containsKey(block)) {
							if (n == out.length) {
								out = Arrays.copyOf(out, n * 2);
								outBlocks = Arrays.copyOf(outBlocks, n * 2);
							}
							out[n] = BlockPos.asLong(cp.getStartX() + x, baseY + y, cp.getStartZ() + z);
							outBlocks[n] = block;
							n++;
						}
					}
				}
			}
		}
		return new Scan(ticks, scanVersion, Arrays.copyOf(out, n), Arrays.copyOf(outBlocks, n));
	}

	// ---------------------------------------------------------------- merge

	private BoxList build(ClientWorld world, BlockPos player, SxConfig cfg, boolean predicted) {
		Map<Block, Integer> wanted = selectedBlocks;
		LongOpenHashSet seen = new LongOpenHashSet();
		long[] pos = new long[256];
		int[] rgb = new int[256];
		int n = 0;
		int px = player.getX(), py = player.getY(), pz = player.getZ();
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int pcx = px >> 4, pcz = pz >> 4;

		// real blocks in loaded chunks (always right, also finds anything a server shows)
		if (cfg.scanLoadedChunks) {
			for (Long2ObjectOpenHashMap.Entry<Scan> entry : scans.long2ObjectEntrySet()) {
				Scan scan = entry.getValue();
				for (int i = 0; i < scan.positions.length; i++) {
					long p = scan.positions[i];
					mutable.set(p);
					if (world.getBlockState(mutable).getBlock() != scan.blocks[i]) continue; // mined / replaced since the scan
					if (!seen.add(p)) continue;
					if (n == pos.length) {
						pos = Arrays.copyOf(pos, n * 2);
						rgb = Arrays.copyOf(rgb, n * 2);
					}
					pos[n] = p;
					rgb[n] = wanted.get(scan.blocks[i]);
					n++;
				}
			}
		}

		// ores predicted from the seed (the server's anti-xray cannot hide these)
		if (predicted) {
			int r = cfg.oreRadius + 1;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					OreChunk chunk = SeedXray.predictor.ore(pcx + dx, pcz + dz);
					if (chunk == null) continue;
					for (int i = 0; i < chunk.positions.length; i++) {
						Integer color = wanted.get(chunk.blocks[i]);
						if (color == null) continue;
						long p = chunk.positions[i];
						if (!seen.add(p)) continue;
						mutable.set(p);
						// where the real chunk is loaded, caves/lava/water show our guess was wrong (e.g. carved away)
						WorldChunk loaded = world.getChunkManager().getWorldChunk(mutable.getX() >> 4, mutable.getZ() >> 4);
						if (loaded != null) {
							BlockState real = loaded.getBlockState(mutable);
							if (real.isAir() || !real.getFluidState().isEmpty()) continue;
						}
						if (n == pos.length) {
							pos = Arrays.copyOf(pos, n * 2);
							rgb = Arrays.copyOf(rgb, n * 2);
						}
						pos[n] = p;
						rgb[n] = color;
						n++;
					}
				}
			}
		}
		if (n == 0) return BoxList.EMPTY;

		// nearest first, at most maxBoxes
		long[] order = new long[n];
		for (int i = 0; i < n; i++) {
			long p = pos[i];
			long dx = BlockPos.unpackLongX(p) - px, dy = BlockPos.unpackLongY(p) - py, dz = BlockPos.unpackLongZ(p) - pz;
			long d2 = dx * dx + dy * dy + dz * dz;
			order[i] = (d2 << 24) | i; // n < 2^24
		}
		Arrays.sort(order);
		int count = Math.min(n, cfg.maxBoxes);
		int[] xs = new int[count], ys = new int[count], zs = new int[count], cs = new int[count];
		for (int i = 0; i < count; i++) {
			int idx = (int) (order[i] & 0xFFFFFF);
			long p = pos[idx];
			xs[i] = BlockPos.unpackLongX(p);
			ys[i] = BlockPos.unpackLongY(p);
			zs[i] = BlockPos.unpackLongZ(p);
			cs[i] = rgb[idx];
		}
		return new BoxList(xs, ys, zs, cs, count);
	}
}

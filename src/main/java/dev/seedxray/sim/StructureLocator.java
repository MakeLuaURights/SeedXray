package dev.seedxray.sim;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.SimpleStructurePiece;
import net.minecraft.structure.StrongholdGenerator;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;

/**
 * Finds structure starts for a seed by running the exact vanilla placement code (spacing/separation,
 * salts, exclusion zones, weighted variants, biome checks), the same way the game does it while generating chunks.
 * Not thread safe: use from one worker thread.
 */
public final class StructureLocator {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Object NONE = new Object();

	private final DimensionSim sim;
	/** structure set -> (start chunk -> StructureHit or NONE). */
	private final Map<StructureSet, Long2ObjectOpenHashMap<Object>> cache = new IdentityHashMap<>();
	private int cacheSize;

	public StructureLocator(DimensionSim sim) {
		this.sim = sim;
	}

	/** Convenience for tests and one-shot use. */
	public List<StructureHit> locate(BlockPos center, int radiusChunks, Set<String> wanted) {
		List<StructureHit> out = new ArrayList<>();
		locate(center, radiusChunks, wanted, out::add, () -> false);
		return out;
	}

	private record Candidate(StructureSet set, StructurePlacement placement, ChunkPos start, long distance) {
	}

	/**
	 * Structure starts within {@code radiusChunks} of {@code center} whose id is in {@code wanted}, nearest first,
	 * each handed to {@code sink} as soon as it is known. Concentric-ring structures (strongholds) are returned
	 * whole: there are only 128 of them.
	 */
	public void locate(BlockPos center, int radiusChunks, Set<String> wanted, Consumer<StructureHit> sink, BooleanSupplier cancelled) {
		int ccx = center.getX() >> 4;
		int ccz = center.getZ() >> 4;
		List<Candidate> candidates = new ArrayList<>();
		for (RegistryEntry<StructureSet> setEntry : sim.placements.getStructureSets()) {
			StructureSet set = setEntry.value();
			if (!isRelevant(set, wanted)) continue;
			StructurePlacement placement = set.placement();
			if (placement instanceof RandomSpreadStructurePlacement spread) {
				int spacing = spread.getSpacing();
				int minRx = Math.floorDiv(ccx - radiusChunks, spacing) - 1;
				int maxRx = Math.floorDiv(ccx + radiusChunks, spacing) + 1;
				int minRz = Math.floorDiv(ccz - radiusChunks, spacing) - 1;
				int maxRz = Math.floorDiv(ccz + radiusChunks, spacing) + 1;
				for (int rx = minRx; rx <= maxRx; rx++) {
					for (int rz = minRz; rz <= maxRz; rz++) {
						ChunkPos start = spread.getStartChunk(sim.seed, rx * spacing, rz * spacing);
						if (Math.abs(start.x - ccx) > radiusChunks || Math.abs(start.z - ccz) > radiusChunks) continue;
						candidates.add(new Candidate(set, spread, start, dist2(start, ccx, ccz)));
					}
				}
			} else if (placement instanceof ConcentricRingsStructurePlacement rings) {
				List<ChunkPos> positions = sim.placements.getPlacementPositions(rings);
				if (positions != null) {
					for (ChunkPos start : positions) {
						candidates.add(new Candidate(set, rings, start, dist2(start, ccx, ccz)));
					}
				}
			}
		}
		candidates.sort(java.util.Comparator.comparingLong(Candidate::distance));
		for (Candidate c : candidates) {
			if (cancelled.getAsBoolean()) return;
			StructureHit hit = hitAt(c.set, c.placement, c.start);
			if (hit != null && matches(hit, wanted)) sink.accept(hit);
		}
	}

	private static long dist2(ChunkPos pos, int cx, int cz) {
		long dx = pos.x - cx;
		long dz = pos.z - cz;
		return dx * dx + dz * dz;
	}

	private static boolean matches(StructureHit hit, Set<String> wanted) {
		return wanted.contains(hit.id)
			|| (hit.id.equals("stronghold") && wanted.contains(StructureCatalog.END_PORTAL))
			|| (hit.id.equals("end_city") && wanted.contains(StructureCatalog.END_SHIP_ELYTRA));
	}

	private StructureHit hitAt(StructureSet set, StructurePlacement placement, ChunkPos start) {
		Long2ObjectOpenHashMap<Object> perSet = cache.computeIfAbsent(set, k -> new Long2ObjectOpenHashMap<>());
		Object cached = perSet.get(start.toLong());
		if (cached == null) {
			if (++cacheSize > 4000) { // the player travelled a long way; start over rather than grow forever
				cache.clear();
				cacheSize = 0;
				perSet = cache.computeIfAbsent(set, k -> new Long2ObjectOpenHashMap<>());
			}
			cached = NONE;
			if (placement.shouldGenerate(sim.placements, start.x, start.z)) {
				StructureHit hit = startAt(set, start);
				if (hit != null) cached = hit;
			}
			perSet.put(start.toLong(), cached);
		}
		return cached instanceof StructureHit hit ? hit : null;
	}

	private boolean isRelevant(StructureSet set, Set<String> wanted) {
		for (StructureSet.WeightedEntry e : set.structures()) {
			String id = idOf(e.structure());
			if (wanted.contains(id)) return true;
			if (id.equals("stronghold") && wanted.contains(StructureCatalog.END_PORTAL)) return true;
			if (id.equals("end_city") && wanted.contains(StructureCatalog.END_SHIP_ELYTRA)) return true;
		}
		return false;
	}

	/** Mirrors {@code ChunkGenerator.setStructureStarts}. */
	private StructureHit startAt(StructureSet set, ChunkPos chunk) {
		List<StructureSet.WeightedEntry> entries = set.structures();
		if (entries.size() == 1) {
			return tryStart(entries.get(0), chunk);
		}
		List<StructureSet.WeightedEntry> pool = new ArrayList<>(entries);
		ChunkRandom random = new ChunkRandom(new CheckedRandom(0L));
		random.setCarverSeed(sim.placements.getStructureSeed(), chunk.x, chunk.z);
		int total = 0;
		for (StructureSet.WeightedEntry e : pool) total += e.weight();
		while (!pool.isEmpty()) {
			int roll = random.nextInt(total);
			int index = 0;
			for (StructureSet.WeightedEntry e : pool) {
				roll -= e.weight();
				if (roll < 0) break;
				index++;
			}
			StructureSet.WeightedEntry chosen = pool.get(index);
			StructureHit hit = tryStart(chosen, chunk);
			if (hit != null) return hit;
			pool.remove(index);
			total -= chosen.weight();
		}
		return null;
	}

	private StructureHit tryStart(StructureSet.WeightedEntry entry, ChunkPos chunk) {
		Structure structure = entry.structure().value();
		Predicate<RegistryEntry<Biome>> biomeOk = structure.getValidBiomes()::contains;
		try {
			Structure.Context context = new Structure.Context(
				sim.registries, sim.generator, sim.biomeSource, sim.noise, WorldgenData.get().templates, sim.seed, chunk, sim.heightView, biomeOk
			);
			Optional<Structure.StructurePosition> position = structure.getValidStructurePosition(context);
			if (position.isPresent()) {
				// Like vanilla's StructureStart.hasChildren(): jigsaw structures can fail to assemble.
				List<StructurePiece> pieces = position.get().generate().toList().pieces();
				if (!pieces.isEmpty()) {
					return new StructureHit(idOf(entry.structure()), position.get().position(), chunk, pieces);
				}
			}
		} catch (Exception e) {
			LOGGER.warn("[SeedXray] Could not evaluate {} at {}: {}", idOf(entry.structure()), chunk, e.toString());
		}
		return null;
	}

	private static String idOf(RegistryEntry<Structure> entry) {
		return entry.getKey().map(k -> k.getValue().getPath()).orElse("?");
	}

	/**
	 * Generates the pieces of a stronghold / end city to find the exact end portal / elytra frame.
	 * Result is stored in {@link StructureHit#detail()}.
	 */
	public void detail(StructureHit hit) {
		if (hit.detailDone) return;
		hit.detailDone = true;
		try {
			List<StructurePiece> pieces = hit.pieces;
			if (hit.id.equals("stronghold")) {
				for (StructurePiece piece : pieces) {
					if (piece instanceof StrongholdGenerator.PortalRoom room) {
						BlockPos portal = new BlockPos(room.applyXTransform(5, 10), room.applyYTransform(3), room.applyZTransform(5, 10));
						hit.detail = new StructureHit.StructureDetail(portal, null);
						break;
					}
				}
			} else if (hit.id.equals("end_city")) {
				for (StructurePiece piece : pieces) {
					if (piece instanceof SimpleStructurePiece simple) {
						BlockPos elytra = findElytraMarker(simple);
						if (elytra != null) {
							hit.detail = new StructureHit.StructureDetail(null, elytra);
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			LOGGER.warn("[SeedXray] Could not generate pieces of {}: {}", hit.id, e.toString());
		}
	}

	private static BlockPos findElytraMarker(SimpleStructurePiece piece) {
		for (StructureTemplate.StructureBlockInfo info : piece.getTemplate().getInfosForBlock(piece.getPos(), piece.getPlacementData(), Blocks.STRUCTURE_BLOCK)) {
			NbtCompound nbt = info.nbt();
			if (nbt != null && nbt.getString("metadata", "").startsWith("Elytra")) {
				return info.pos();
			}
		}
		return null;
	}
}

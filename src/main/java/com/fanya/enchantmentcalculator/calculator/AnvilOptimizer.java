package com.fanya.enchantmentcalculator.calculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** поиск самой дешёвой комбинации */
public final class AnvilOptimizer {
    public record Leaf(String name, int value, int work, boolean item) {
    }

    public record Operand(String name, int mask) {
    }

    public record Merge(
            Operand left,
            Operand right,
            int levels,
            int experience,
            int priorPenalty
    ) {
    }

    public record Result(List<Merge> steps, int totalLevels, int totalExperience) {
    }

    private record State(
            int mask,
            int value,
            int work,
            boolean item,
            String name,
            int levels,
            int experience,
            State left,
            State right,
            int mergeLevels,
            int mergeExperience,
            int priorPenalty
    ) {
    }

    private AnvilOptimizer() {
    }

    public static Result solve(List<Leaf> leaves, OptimizationMode mode, int maxMergeLevels) {
        int count = leaves.size();
        if (count < 2 || count > 16) {
            throw new IllegalArgumentException("Expected 2 to 16 operands");
        }

        int full = (1 << count) - 1;
        @SuppressWarnings("unchecked")
        Map<Integer, State>[] best = new Map[full + 1];
        for (int mask = 1; mask <= full; mask++) {
            best[mask] = new HashMap<>();
        }

        for (int i = 0; i < count; i++) {
            Leaf leaf = leaves.get(i);
            if (leaf.work() < 0 || leaf.value() < 0) {
                throw new IllegalArgumentException("Negative input");
            }
            State initial = new State(
                    1 << i,
                    leaf.value(),
                    leaf.work(),
                    leaf.item(),
                    leaf.name(),
                    0,
                    0,
                    null,
                    null,
                    0,
                    0,
                    0
            );
            best[1 << i].put(leaf.work(), initial);
        }

        for (int mask = 1; mask <= full; mask++) {
            if (Integer.bitCount(mask) == 1) {
                continue;
            }

            for (int leftMask = (mask - 1) & mask; leftMask > 0; leftMask = (leftMask - 1) & mask) {
                int rightMask = mask ^ leftMask;
                for (State left : best[leftMask].values()) {
                    for (State right : best[rightMask].values()) {
                        if (right.item) {
                            continue;
                        }

                        State candidate = merge(left, right, maxMergeLevels);
                        if (candidate != null) {
                            keep(best[mask], candidate, mode);
                        }
                    }
                }
            }
        }

        State winner = null;
        for (State candidate : best[full].values()) {
            if (candidate.item && (winner == null || better(candidate, winner, mode))) {
                winner = candidate;
            }
        }
        if (winner == null) {
            return null;
        }

        List<Merge> steps = new ArrayList<>();
        collect(winner, steps);
        return new Result(List.copyOf(steps), winner.levels, winner.experience);
    }

    private static State merge(State left, State right, int max) {
        int prior = priorPenalty(left.work) + priorPenalty(right.work);
        int cost = right.value + prior;
        if (cost > max || cost < 0) {
            return null;
        }

        int xp = experience(cost);
        return new State(
                left.mask | right.mask,
                left.value + right.value,
                Math.max(left.work, right.work) + 1,
                left.item,
                left.name + " + " + right.name,
                left.levels + right.levels + cost,
                left.experience + right.experience + xp,
                left,
                right,
                cost,
                xp,
                prior
        );
    }

    private static int priorPenalty(int work) {
        if (work >= 30) {
            return Integer.MAX_VALUE / 4;
        }
        return (1 << work) - 1;
    }

    private static void keep(Map<Integer, State> states, State candidate, OptimizationMode mode) {
        State old = states.get(candidate.work);
        if (old == null || better(candidate, old, mode)) {
            states.put(candidate.work, candidate);
        }
    }

    private static boolean better(State first, State second, OptimizationMode mode) {
        int a = mode == OptimizationMode.EXPERIENCE ? first.experience : first.levels;
        int b = mode == OptimizationMode.EXPERIENCE ? second.experience : second.levels;
        if (a != b) {
            return a < b;
        }
        if (mode == OptimizationMode.EXPERIENCE) {
            return first.levels < second.levels;
        }
        return first.experience < second.experience;
    }

    private static void collect(State state, List<Merge> steps) {
        if (state.left == null) {
            return;
        }

        collect(state.left, steps);
        collect(state.right, steps);
        steps.add(new Merge(
                new Operand(state.left.name, state.left.mask),
                new Operand(state.right.name, state.right.mask),
                state.mergeLevels,
                state.mergeExperience,
                state.priorPenalty
        ));
    }

    public static int experience(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (5 * level * level - 81 * level + 720) / 2;
        }
        return (9 * level * level - 325 * level + 4440) / 2;
    }
}

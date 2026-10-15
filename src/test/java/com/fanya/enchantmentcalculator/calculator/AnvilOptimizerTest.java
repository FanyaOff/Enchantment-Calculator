package com.fanya.enchantmentcalculator.calculator;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnvilOptimizerTest {
    @Test
    void priorWorkPenaltyUsesTheExistingItemCost() {
        List<AnvilOptimizer.Leaf> leaves = List.of(
                new AnvilOptimizer.Leaf("Sword", 0, 2, true),
                new AnvilOptimizer.Leaf("Sharpness", 2, 0, false)
        );

        AnvilOptimizer.Result result = AnvilOptimizer.solve(leaves, OptimizationMode.LEVELS, 39);

        assertEquals(5, result.totalLevels());
        assertEquals(1, result.steps().size());
        assertEquals(3, result.steps().get(0).priorPenalty());
    }

    @Test
    void noStepMayExceedTheSurvivalAnvilLimit() {
        List<AnvilOptimizer.Leaf> leaves = List.of(
                new AnvilOptimizer.Leaf("Sword", 0, 0, true),
                new AnvilOptimizer.Leaf("Impossible book", 40, 0, false)
        );

        assertNull(AnvilOptimizer.solve(leaves, OptimizationMode.LEVELS, 39));
    }

    @Test
    void subsetSearchMatchesExhaustiveMergeSearchForBothObjectives() {
        List<AnvilOptimizer.Leaf> leaves = List.of(
                new AnvilOptimizer.Leaf("Sword", 0, 0, true),
                new AnvilOptimizer.Leaf("A", 8, 0, false),
                new AnvilOptimizer.Leaf("B", 5, 0, false),
                new AnvilOptimizer.Leaf("C", 3, 0, false),
                new AnvilOptimizer.Leaf("D", 1, 0, false)
        );

        for (OptimizationMode mode : OptimizationMode.values()) {
            AnvilOptimizer.Result result = AnvilOptimizer.solve(leaves, mode, 39);
            int expected = exhaustiveCost(initialStates(leaves), mode, 39);
            int actual = mode == OptimizationMode.LEVELS
                    ? result.totalLevels()
                    : result.totalExperience();

            assertEquals(expected, actual);
        }
    }

    private record BruteState(int value, int work, boolean item, int levels, int experience) {
    }

    private static List<BruteState> initialStates(List<AnvilOptimizer.Leaf> leaves) {
        List<BruteState> states = new ArrayList<>();
        for (AnvilOptimizer.Leaf leaf : leaves) {
            states.add(new BruteState(leaf.value(), leaf.work(), leaf.item(), 0, 0));
        }
        return states;
    }

    private static int exhaustiveCost(List<BruteState> states, OptimizationMode mode, int maximum) {
        if (states.size() == 1) {
            BruteState finalState = states.get(0);
            if (!finalState.item()) {
                return Integer.MAX_VALUE;
            }
            return mode == OptimizationMode.LEVELS
                    ? finalState.levels()
                    : finalState.experience();
        }

        int best = Integer.MAX_VALUE;
        for (int leftIndex = 0; leftIndex < states.size(); leftIndex++) {
            for (int rightIndex = 0; rightIndex < states.size(); rightIndex++) {
                if (leftIndex == rightIndex) {
                    continue;
                }
                BruteState left = states.get(leftIndex);
                BruteState right = states.get(rightIndex);
                if (right.item()) {
                    continue;
                }

                int prior = (1 << left.work()) - 1 + (1 << right.work()) - 1;
                int cost = right.value() + prior;
                if (cost > maximum) {
                    continue;
                }

                BruteState merged = new BruteState(
                        left.value() + right.value(),
                        Math.max(left.work(), right.work()) + 1,
                        left.item(),
                        left.levels() + right.levels() + cost,
                        left.experience() + right.experience() + AnvilOptimizer.experience(cost)
                );
                List<BruteState> next = new ArrayList<>();
                for (int index = 0; index < states.size(); index++) {
                    if (index != leftIndex && index != rightIndex) {
                        next.add(states.get(index));
                    }
                }
                next.add(merged);
                best = Math.min(best, exhaustiveCost(next, mode, maximum));
            }
        }
        return best;
    }
}

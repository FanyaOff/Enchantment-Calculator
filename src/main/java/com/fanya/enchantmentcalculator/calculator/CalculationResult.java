package com.fanya.enchantmentcalculator.calculator;

import java.util.List;

public final class CalculationResult {
    private final List<Step> steps;
    private final int totalLevels;
    private final int totalExperience;

    public CalculationResult(List<Step> steps, int totalLevels, int totalExperience) {
        this.steps = List.copyOf(steps);
        this.totalLevels = totalLevels;
        this.totalExperience = totalExperience;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public int getTotalLevels() {
        return totalLevels;
    }
    public int getTotalExperience() {
        return totalExperience;
    }

    public static final class Step {
        private final String description;
        private final int levels;
        private final int experience;
        private final int priorWorkPenalty;
        private final int leftMask;
        private final int rightMask;

        public Step(
                String description,
                int levels,
                int experience,
                int priorWorkPenalty,
                int leftMask,
                int rightMask
        ) {
            this.description = description;
            this.levels = levels;
            this.experience = experience;
            this.priorWorkPenalty = priorWorkPenalty;
            this.leftMask = leftMask;
            this.rightMask = rightMask;
        }

        public String getDescription() {
            return description;
        }

        public int getLevels() {
            return levels;
        }

        public int getExperience() {
            return experience;
        }

        public int getPriorWorkPenalty() {
            return priorWorkPenalty;
        }
        public int getLeftMask() {
            return leftMask;
        }

        public int getRightMask() {
            return rightMask;
        }
    }
}

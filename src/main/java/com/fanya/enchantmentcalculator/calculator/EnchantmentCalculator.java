package com.fanya.enchantmentcalculator.calculator;

import com.fanya.enchantmentcalculator.EnchantmentCalculatorMod;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.*;

public class EnchantmentCalculator {
    private static final int[] ANVIL_COST_MULTIPLIERS = {0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023};

    public static CalculationResult calculate(ItemStack targetItem, List<EnchantmentCombination> enchantments, OptimizationMode mode) {
        String itemName = targetItem.getName().getString();

        List<EnchantItem> items = new ArrayList<>();

        items.add(new EnchantItem(itemName, 0, true, 0));

        for (EnchantmentCombination enchant : enchantments) {
            int bookCost = getBookEnchantmentCost(enchant.enchantment(), enchant.level());
            String enchantId = getEnchantmentId(enchant.enchantment());
            String enchantName = getEnchantmentDisplayName(enchantId);
            String displayName = enchantName + (enchant.level() > 1 ? " " + enchant.level() : "");

            List<String> bookEnchantments = List.of(displayName);
            String bookName = Text.translatable("enchantmentcalculator.book", displayName).getString();
            items.add(new EnchantItem(bookName, bookCost, false, 0, bookEnchantments));
        }

        TreeResult result = findOptimalTree(items, mode);

        return new CalculationResult(result.steps, result.totalLevels, result.totalExperience);
    }

    private static int getBookEnchantmentCost(Enchantment enchantment, int level) {
        String id = getEnchantmentId(enchantment);
        int weight = getReferenceWeight(id);

        return level * weight;
    }

    private static int getReferenceWeight(String enchantId) {
        return switch (enchantId) {

            case "aqua_affinity", "blast_protection", "depth_strider", "fire_aspect",
                 "flame", "frost_walker", "impaling", "luck_of_the_sea", "lure",
                 "mending", "multishot", "punch", "respiration", "riptide",
                 "breach", "wind_burst", "looting", "fortune", "sweeping_edge" -> 2;

            case "soul_speed", "swift_sneak", "thorns", "binding_curse",
                 "vanishing_curse", "silk_touch", "infinity", "channeling" -> 4;

            default -> 1;
        };
    }

    private static TreeResult findOptimalTree(List<EnchantItem> items, OptimizationMode mode) {
        int n = items.size();

        if (n <= 2) {
            return calculateSimpleCombination(items);
        }

        List<TreeStructure> optimalTrees = generateOptimalTreeStructures(n);

        TreeResult bestResult = null;
        int bestCost = Integer.MAX_VALUE;

        for (TreeStructure tree : optimalTrees) {

            List<EnchantItem> optimizedItems = optimizeItemPlacement(items, tree);

            TreeResult result = calculateTreeCost(tree, optimizedItems);

            int cost = (mode == OptimizationMode.LEVELS) ? result.totalLevels : result.totalExperience;


            if (cost < bestCost) {
                bestCost = cost;
                bestResult = result;
            }
        }

        return bestResult;
    }

    private static List<TreeStructure> generateOptimalTreeStructures(int n) {
        List<TreeStructure> allTrees = generateAllTreeStructures(n);

        Map<Integer, TreeStructure> bestByAnvilCost = new HashMap<>();

        for (TreeStructure tree : allTrees) {
            int anvilCost = tree.getMaxDepth();
            List<Integer> contributions = tree.getLeafContributions();

            int weightedSum = 0;
            for (int i = 0; i < contributions.size(); i++) {
                weightedSum += i * contributions.get(i);
            }

            if (!bestByAnvilCost.containsKey(anvilCost) ||
                    weightedSum < bestByAnvilCost.get(anvilCost).getWeightedSum()) {
                bestByAnvilCost.put(anvilCost, tree);
            }
        }
        return new ArrayList<>(bestByAnvilCost.values());
    }


    private static List<EnchantItem> optimizeItemPlacement(List<EnchantItem> items, TreeStructure tree) {
        List<EnchantItem> books = new ArrayList<>();
        EnchantItem mainItem = null;

        for (EnchantItem item : items) {
            if (item.isItem) {
                mainItem = item;
            } else {
                books.add(item);
            }
        }

        List<Integer> contributions = tree.getLeafContributions();

        books.sort((a, b) -> Integer.compare(b.enchantmentCost, a.enchantmentCost));

        List<Integer> bookPositions = new ArrayList<>();
        for (int i = 0; i < contributions.size(); i++) {
            if (contributions.get(i) != 0) {
                bookPositions.add(i);
            }
        }

        bookPositions.sort(Comparator.comparingInt(contributions::get));

        List<EnchantItem> result = new ArrayList<>(Collections.nCopies(items.size(), null));

        int mainItemPosition = contributions.indexOf(0);
        if (mainItemPosition == -1) {
            mainItemPosition = 0;
        }
        result.set(mainItemPosition, mainItem);

        for (int i = 0; i < books.size() && i < bookPositions.size(); i++) {
            int position = bookPositions.get(i);
            result.set(position, books.get(i));
        }

        for (int i = 0; i < result.size(); i++) {
            if (result.get(i) == null) {
                EnchantmentCalculatorMod.LOGGER.error("Position {} not filled! Contributions: {}", i, contributions);
                for (EnchantItem item : items) {
                    if (!result.contains(item)) {
                        result.set(i, item);
                        break;
                    }
                }
            }
        }

        return result;
    }

    private static TreeResult calculateTreeCost(TreeStructure tree, List<EnchantItem> items) {
        List<CalculationResult.Step> steps = new ArrayList<>();
        buildTreeWithItems(tree, items, 0, steps);

        int totalLevels = 0;
        int totalExperience = 0;

        for (CalculationResult.Step step : steps) {
            totalLevels += step.getLevels();
            totalExperience += step.getExperience();
        }

        return new TreeResult(steps, totalLevels, totalExperience);
    }

    private static TreeNode buildTreeWithItems(TreeStructure structure, List<EnchantItem> items, int startIndex, List<CalculationResult.Step> steps) {
        if (structure.left == null && structure.right == null) {
            if (startIndex >= items.size()) {
                EnchantmentCalculatorMod.LOGGER.error("Index {} is unbound from list {}", startIndex, items.size());
                return null;
            }
            return new TreeNode(items.get(startIndex), 0, 0);
        }

        assert structure.left != null;
        TreeNode leftNode = buildTreeWithItems(structure.left, items, startIndex, steps);
        int rightStartIndex = startIndex + getLeafCount(structure.left);
        TreeNode rightNode = buildTreeWithItems(structure.right, items, rightStartIndex, steps);

        if (leftNode == null || rightNode == null || leftNode.item == null || rightNode.item == null) {
            EnchantmentCalculatorMod.LOGGER.error("one of node is null: leftNode={}, rightNode={}", leftNode, rightNode);
            return null;
        }

        int enchantmentCost = rightNode.item.enchantmentCost;
        int leftAnvilCost = leftNode.anvilCost;
        int rightAnvilCost = rightNode.anvilCost;
        int newAnvilCost = Math.max(leftAnvilCost, rightAnvilCost) + 1;

        int priorWorkPenalty = 0;
        if (leftAnvilCost < ANVIL_COST_MULTIPLIERS.length) {
            priorWorkPenalty += ANVIL_COST_MULTIPLIERS[leftAnvilCost];
        }
        if (rightAnvilCost < ANVIL_COST_MULTIPLIERS.length) {
            priorWorkPenalty += ANVIL_COST_MULTIPLIERS[rightAnvilCost];
        }

        int mergeCost = enchantmentCost + priorWorkPenalty;

        String leftName = leftNode.item.name;
        String rightName = rightNode.item.name;

        int experience = calculateExperience(mergeCost);
        String description = Text.translatable("enchantmentcalculator.combine", leftName, rightName).getString();
        steps.add(new CalculationResult.Step(description, mergeCost, experience, priorWorkPenalty));

        String combinedName;
        List<String> combinedEnchantments = new ArrayList<>();

        if (leftNode.item.isItem) {
            combinedName = getBaseItemName(leftNode.item.name);

            combinedEnchantments.addAll(leftNode.item.enchantments);
            combinedEnchantments.addAll(rightNode.item.enchantments);

            if (!combinedEnchantments.isEmpty()) {
                combinedName = combinedName + " (" + String.join(", ", combinedEnchantments) + ")";
            }
        } else {
            combinedEnchantments.addAll(leftNode.item.enchantments);
            combinedEnchantments.addAll(rightNode.item.enchantments);

            String enchantmentsList = String.join(", ", combinedEnchantments);
            combinedName = Text.translatable("enchantmentcalculator.book", enchantmentsList).getString();
        }

        EnchantItem combinedItem = new EnchantItem(combinedName,
                leftNode.item.enchantmentCost + rightNode.item.enchantmentCost,
                leftNode.item.isItem,
                newAnvilCost,
                combinedEnchantments);

        return new TreeNode(combinedItem, newAnvilCost, mergeCost);
    }

    private static String getBaseItemName(String itemName) {
        int openParen = itemName.indexOf('(');
        if (openParen != -1) {
            return itemName.substring(0, openParen).trim();
        }
        return itemName;
    }

    private static TreeResult calculateSimpleCombination(List<EnchantItem> items) {
        if (items.size() != 2) {
            throw new IllegalArgumentException("Simple combination requires exactly 2 items");
        }

        List<CalculationResult.Step> steps = new ArrayList<>();

        EnchantItem left = items.get(0);
        EnchantItem right = items.get(1);

        int enchantmentCost = right.enchantmentCost;
        int priorWorkPenalty = ANVIL_COST_MULTIPLIERS[left.anvilCost] + ANVIL_COST_MULTIPLIERS[right.anvilCost];
        int mergeCost = enchantmentCost + priorWorkPenalty;
        int experience = calculateExperience(mergeCost);

        String description = Text.translatable("enchantmentcalculator.combine", left.name, right.name).getString();
        steps.add(new CalculationResult.Step(description, mergeCost, experience, priorWorkPenalty));

        return new TreeResult(steps, mergeCost, experience);
    }

    private static List<TreeStructure> generateAllTreeStructures(int n) {
        if (n == 1) {
            return List.of(new TreeStructure(0, null, null));
        }

        List<TreeStructure> trees = new ArrayList<>();

        for (int leftSize = 1; leftSize < n; leftSize++) {
            int rightSize = n - leftSize;

            List<TreeStructure> leftTrees = generateAllTreeStructures(leftSize);
            List<TreeStructure> rightTrees = generateAllTreeStructures(rightSize);

            for (TreeStructure left : leftTrees) {
                for (TreeStructure right : rightTrees) {
                    trees.add(new TreeStructure(0, left, right));
                }
            }
        }

        return trees;
    }

    private static int getLeafCount(TreeStructure structure) {
        if (structure.left == null && structure.right == null) {
            return 1;
        }
        assert structure.left != null;
        return getLeafCount(structure.left) + getLeafCount(structure.right);
    }

    private static int calculateExperience(int level) {
        if (level == 0) return 0;
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int)(2.5 * level * level - 40.5 * level + 360);
        return (int)(4.5 * level * level - 162.5 * level + 2220);
    }

    private static String getEnchantmentId(Enchantment enchantment) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            try {
                RegistryEntry<Enchantment> entry = client.world.getRegistryManager()
                        .getWrapperOrThrow(RegistryKeys.ENCHANTMENT)
                        .streamEntries()
                        .filter(e -> e.value() == enchantment)
                        .findFirst()
                        .orElse(null);

                if (entry != null) {
                    return entry.getKey().map(key -> key.getValue().getPath()).orElse("unknown");
                }
            } catch (Exception ignored) {}
        }
        return "unknown";
    }

    private static String getEnchantmentDisplayName(String id) {
        try {
            return Text.translatable("enchantment.minecraft." + id).getString();
        } catch (Exception e) {
            return id.replace("_", " ");
        }
    }
    // help classes

    private static class EnchantItem {
        String name;
        int enchantmentCost;
        boolean isItem;
        int anvilCost;
        List<String> enchantments;

        EnchantItem(String name, int enchantmentCost, boolean isItem, int anvilCost) {
            this.name = name;
            this.enchantmentCost = enchantmentCost;
            this.isItem = isItem;
            this.anvilCost = anvilCost;
            this.enchantments = new ArrayList<>();
        }

        EnchantItem(String name, int enchantmentCost, boolean isItem, int anvilCost, List<String> enchantments) {
            this.name = name;
            this.enchantmentCost = enchantmentCost;
            this.isItem = isItem;
            this.anvilCost = anvilCost;
            this.enchantments = new ArrayList<>(enchantments);
        }
    }


    private static class TreeStructure {
        int depth;
        TreeStructure left;
        TreeStructure right;

        TreeStructure(int depth, TreeStructure left, TreeStructure right) {
            this.depth = depth;
            this.left = left;
            this.right = right;
        }

        int getMaxDepth() {
            if (left == null && right == null) {
                return 0;
            }
            assert left != null;
            return Math.max(left.getMaxDepth(), right.getMaxDepth()) + 1;
        }

        List<Integer> getLeafContributions() {
            List<Integer> contributions = new ArrayList<>();
            collectContributions(contributions, 0);
            return contributions;
        }

        int getWeightedSum() {
            List<Integer> contributions = getLeafContributions();
            int weightedSum = 0;
            for (int i = 0; i < contributions.size(); i++) {
                weightedSum += i * contributions.get(i);
            }
            return weightedSum;
        }

        private void collectContributions(List<Integer> contributions, int currentContribution) {
            if (left == null && right == null) {
                contributions.add(currentContribution);
                return;
            }

            if (left != null) {
                left.collectContributions(contributions, currentContribution);
            }
            if (right != null) {
                right.collectContributions(contributions, currentContribution + 1);
            }
        }
    }


    private static class TreeNode {
        EnchantItem item;
        int anvilCost;
        int totalCost;

        TreeNode(EnchantItem item, int anvilCost, int totalCost) {
            this.item = item;
            this.anvilCost = anvilCost;
            this.totalCost = totalCost;
        }
    }

    private static class TreeResult {
        List<CalculationResult.Step> steps;
        int totalLevels;
        int totalExperience;

        TreeResult(List<CalculationResult.Step> steps, int totalLevels, int totalExperience) {
            this.steps = steps;
            this.totalLevels = totalLevels;
            this.totalExperience = totalExperience;
        }
    }
}

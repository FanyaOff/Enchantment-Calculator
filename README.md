## Overview

Enchantment Calculator is a client-side mod that helps you find the most cost-effective way to combine enchantments on your items.
This mod is based on the functionality provided by [this website](https://iamcal.github.io/enchant-order/)

This mod supports customization via a custom resource pack. UI elements are located at:
`assets/enchantmentcalculator/textures/gui`
You can view the original textures in the source code on [GitHub](https://github.com/FanyaOff/Enchantment-Calculator/tree/main/src/main/resources/assets/enchantmentcalculator/textures/gui):

## Known bugs
- The interface overflows off-screen when using UI scale 4 (idk how to fix this :P)

## Usage

1. **Place an item** in the anvil
2. **Select enchantments** from the left panel
3. **Click Calculate** to find the optimal sequence
4. **Follow the steps** shown in the right panel
5. **Navigate** through steps using arrow buttons

## Screenshot

<img width="1209" height="423" alt="image" src="https://github.com/user-attachments/assets/b4ab4176-93f9-4317-98fa-0e2dda540c28" />

# Technical Algorithm Overview

## Optimization Algorithm

The Enchantment Calculator uses a sophisticated **tree-based optimization algorithm** to find the most cost-effective enchanting sequence.

### Core Algorithm Steps:

#### 1. **Tree Structure Generation**
```
For n enchantments, generate all possible binary combination trees
- Each leaf = enchantment book or base item
- Each internal node = anvil combination operation
- Tree depth determines maximum prior work penalty
```

#### 2. **Cost Calculation Model**
The mod calculates costs using Minecraft's exact anvil mechanics:

**Total Cost = Enchantment Cost + Prior Work Penalty**

Where:
- **Enchantment Cost**: Based on enchantment rarity weights
- **Prior Work Penalty**: `MULTIPLIERS[left_uses] + MULTIPLIERS[right_uses]`
- **Multipliers**: `[1023]`

#### 3. **Tree Pruning Optimization**
Instead of testing all possible trees, the algorithm:
- Groups trees by maximum depth (anvil use count)
- Calculates weighted contribution sums for each tree
- Keeps only the best tree per depth level
- Reduces complexity from O(n!) to O(n²)

#### 4. **Item Placement Optimization**
For each tree structure:
```
1. Sort enchantment books by cost (expensive first)
2. Calculate leaf contribution values for each position
3. Place expensive items at positions with lowest contributions
4. Minimize total weighted cost
```

#### 5. **Multi-Mode Optimization**
Supports two optimization targets:
- **Level Mode**: Minimizes total anvil level cost
- **Experience Mode**: Minimizes total XP expenditure

### Mathematical Foundation

#### Prior Work Penalty Calculation:
```
penalty = MULTIPLIERS[left_item_uses] + MULTIPLIERS[right_item_uses]
```

#### Experience Cost Conversion:
```
if (levels ≤ 16): xp = levels² + 6×levels
if (levels ≤ 31): xp = 2.5×levels² - 40.5×levels + 360
if (levels > 31): xp = 4.5×levels² - 162.5×levels + 2220
```

#### Tree Contribution Weighting:
```
For each tree position i:
contribution[i] = depth_from_root
weighted_sum = Σ(i × contribution[i])
```

### Algorithm Complexity

- **Time Complexity**: O(n² × 2^n) where n = number of enchantments
- **Space Complexity**: O(n × 2^n) for tree storage
- **Practical Performance**: <100ms for typical 5-7 enchantments

### Enchantment Weight System

The mod uses empirically determined weights matching Minecraft's internal costs:

```java
Weight 1: protection, sharpness, efficiency, unbreaking, power
Weight 2: fire_aspect, looting, fortune, punch, flame
Weight 4: mending, silk_touch, infinity, curse_of_binding
```

### Tree Search Strategy

1. **Generate** all possible binary tree structures
2. **Filter** by maximum depth to reduce search space  
3. **Score** each tree using weighted contribution analysis
4. **Select** optimal trees per depth level
5. **Test** item placements for each selected tree
6. **Return** globally optimal solution

### Edge Case Handling

- **Single enchantment**: Direct application (cost = weight × level)
- **Two enchantments**: Simple combination without tree search
- **Incompatible enchantments**: Filtered during selection phase
- **Maximum anvil uses**: Capped at 6 uses (cost = 1023 levels)

This algorithm ensures you get the mathematically optimal enchanting sequence while maintaining fast performance suitable for real-time gameplay.

## Bug Reports & Feature Requests

Found a bug or have a suggestion? Please report it my our [GitHub Issues](https://github.com/FanyaOff/Enchantment-Calculator/issues) page.


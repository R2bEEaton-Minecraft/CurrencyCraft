import java.util.ArrayList;
import java.util.List;

public class GenerateRecipes {
    public static int[] denominations = {
        1, 2, 5, 10, 20, 50, 100, 
        200, 500, 1000, 2000, 5000, 
        10000, 20000, 50000
    };

    public static String[] denomination_names = {
        "one_cent_coin",
        "two_cent_coin",
        "five_cent_coin",
        "ten_cent_coin",
        "twenty_cent_coin",
        "fifty_cent_coin",
        "one_unit_coin",
        "two_unit_coin",
        "five_unit_note",
        "ten_unit_note",
        "twenty_unit_note",
        "fifty_unit_note",
        "one_hundred_unit_note",
        "two_hundred_unit_note",
        "five_hundred_unit_note",
    };

    public static void main(String[] args) {
        // for (int i = 0; i < denominations.length; i++) {
        //     System.out.println(denomination_names[i]);
        //     List<Integer> combination = new ArrayList<>();
        //     generate(denominations[i], denominations[i], 0, combination, 0);
        // }

        for (int i = denominations.length - 1; i >= 0; i--) {
            for (int j = i - 1; j >= 0; j--) {
                if ((float) denominations[i] / denominations[j] % 1 == 0) {
                    String recipeJson = String.format("""
{
    "type": "minecraft:crafting_shapeless",
    "group": "currencycraft:%s_%s",
    "ingredients": [
        {
            "item": "currencycraft:%s"
        }
    ],
    "result": {
        "item": "currencycraft:%s",
        "count": %s
    }
}
    """, denomination_names[i], "exchange_down", denomination_names[i], denomination_names[j], denominations[i] / denominations[j]);
        try {
            StringBuilder fileName = new StringBuilder();
            fileName.append(denomination_names[i]);
            fileName.append(".json");
            java.nio.file.Files.write(
                java.nio.file.Paths.get(fileName.toString()),
                recipeJson.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
                    break;
                }
            }
        }
    }

    private static void generate(int target, int originalTarget, int startIndex, List<Integer> current, int coinCount) {
        if (target == 0 && coinCount <= 9) {
            // Skip the "single coin equals the target" case
            if (!(coinCount == 1 && current.get(0) == originalTarget)) {
                saveRecipe(originalTarget, current, "exchange_up");
            }
            return;
        }

        if (target < 0 || coinCount >= 9) {
            return;
        }

        for (int i = startIndex; i < denominations.length; i++) {
            current.add(denominations[i]);
            generate(target - denominations[i], originalTarget, i, current, coinCount + 1);
            current.remove(current.size() - 1);
        }
    }

    private static int getIndex(int num) {
        for (int i = 0; i < denominations.length; i++) {
            if (num == denominations[i]) return i;
        }
        return -1;
    }

    private static void saveRecipe(int val, List<Integer> ingredients, String type) {
        StringBuilder ingredientsJson = new StringBuilder();
        ingredientsJson.append("[\n");
        for (int i = 0; i < ingredients.size(); i++) {
            int idx = getIndex(ingredients.get(i));
            ingredientsJson.append("    { \"item\": \"currencycraft:")
            .append(denomination_names[idx]).append("\" }");
            if (i < ingredients.size() - 1) {
            ingredientsJson.append(",");
            }
            ingredientsJson.append("\n");
        }
        ingredientsJson.append("  ]");

        String recipeJson = String.format("""
    {
      "type": "minecraft:crafting_shapeless",
      "group": "currencycraft:%s_%s",
      "ingredients": %s,
      "result": {
        "item": "currencycraft:%s"
      }
    }
    """, denomination_names[getIndex(val)], type, ingredientsJson.toString(), denomination_names[getIndex(val)]);

        try {
            StringBuilder fileName = new StringBuilder();
            fileName.append(denomination_names[getIndex(val)]);
            for (int i = 0; i < ingredients.size(); i++) {
            fileName.append("_").append(getIndex(ingredients.get(i)));
            }
            fileName.append(".json");
            java.nio.file.Files.write(
                java.nio.file.Paths.get(fileName.toString()),
                recipeJson.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

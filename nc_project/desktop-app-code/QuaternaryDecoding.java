import java.util.HashMap;
import java.util.Map;

public class QuaternaryDecoding {
    public static void main(String[] args) {
        // Пример закодированной строки
        String encodedString = "000001002010"; // Замените на вашу закодированную строку
        String decodedString = decodeQuaternary(encodedString);
        System.out.println("Decoded String: " + decodedString);
    }

    private static Map<Character, String> createQuaternaryDictionary() {
        Map<Character, String> dict = new HashMap<>();
        String symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_/";
        int index = 0;

        for (char symbol : symbols.toCharArray()) {
            dict.put(symbol, toQuaternary(index++));
        }

        return dict;
    }

    private static Map<String, Character> createReverseQuaternaryDictionary() {
        Map<String, Character> dict = new HashMap<>();
        String symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_/:.";

        for (int index = 0; index < symbols.length(); index++) {
            dict.put(toQuaternary(index), symbols.charAt(index));
        }

        return dict;
    }

    private static String toQuaternary(int number) {
        StringBuilder quaternary = new StringBuilder();
        while (number > 0) {
            quaternary.insert(0, number % 4);
            number /= 4;
        }
        while (quaternary.length() < 3) {
            quaternary.insert(0, '0');
        }
        return quaternary.toString();
    }

    public static String decodeQuaternary(String encodedString) {
        Map<String, Character> reverseDict = createReverseQuaternaryDictionary();
        StringBuilder decodedString = new StringBuilder();

        // Проходим по строке, беря по 3 символа
        for (int i = 0; i < encodedString.length(); i += 3) {
            String code = encodedString.substring(i, Math.min(i + 3, encodedString.length()));
            decodedString.append(reverseDict.get(code)); // Добавляем соответствующий символ
        }

        return decodedString.toString();
    }
}

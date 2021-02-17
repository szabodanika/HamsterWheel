package hamsterwheel.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class IO {

    static void writeToFile(String filename, boolean overwrite, Object... objects) throws IOException {
        FileWriter fileWriter = null;
        fileWriter = new FileWriter(filename, !overwrite);
        for (Object object : objects) {
            fileWriter.write(object +"\n");
        }
        fileWriter.close();
    }

    static String readFromFile(String filename) throws IOException {
        File file = new File(filename);
        Scanner scanner = new Scanner(file);
        StringBuilder stringBuilder = new StringBuilder();
        while (scanner.hasNext()) {
            stringBuilder.append(scanner.nextLine()).append("\n");
        }
        return stringBuilder.toString();
    }

}

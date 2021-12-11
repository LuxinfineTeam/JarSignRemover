package ml.luxinfine.subscribeclear;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        System.out.print("Enter path to directory with jars: ");
        Path entered = Paths.get(new Scanner(System.in).nextLine());
        if(!Files.isDirectory(entered)) {
            System.out.println("[ERROR] Entered path is not directory!");
            return;
        }
        Path outDir = Paths.get(entered.toString(), "/output");
        if(Files.exists(outDir)) {
            System.out.println("[ERROR] Directory output already exists! Please, delete output directory and restart the program...");
            return;
        }
        try {
            Files.createDirectory(outDir);
            Files.walk(entered, 1)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".jar"))
                    .forEach(jarPath -> {
                        Path result = Paths.get(outDir.toString(), jarPath.getFileName().toString());
                        try {
                            Files.copy(jarPath, result);
                            try (FileSystem fs = FileSystems.newFileSystem(result, null)) {
                                if(Files.deleteIfExists(fs.getPath("/META-INF/SIGNUMO.RSA")))
                                    System.out.println("[INFO] File META-INF/SIGNUMO.RSA in " + jarPath.getFileName() + " successful removed");
                                else
                                    System.out.println("[WARN] File META-INF/SIGNUMO.RSA in " + jarPath.getFileName() + " not exists, skip");

                                if(Files.deleteIfExists(fs.getPath("/META-INF/SIGNUMO.SF")))
                                    System.out.println("[INFO] File META-INF/SIGNUMO.SF in " + jarPath.getFileName() + " successful removed");
                                else
                                    System.out.println("[WARN] File META-INF/SIGNUMO.SF in " + jarPath.getFileName() + " not exists, skip");

                                int size = Main.clearManifest(fs.getPath("/META-INF/MANIFEST.MF"));
                                switch (size) {
                                    case -2:
                                        System.out.println("[WARN] File META-INF/MANIFEST.MF in " + jarPath.getFileName() + " not exists, skip");
                                        break;
                                    case -1:
                                        System.out.println("[WARN] File META-INF/MANIFEST.MF in " + jarPath.getFileName() + " cant rewrite (not access or error)");
                                        break;
                                    case 0:
                                        System.out.println("[INFO] File META-INF/MANIFEST.MF in " + jarPath.getFileName() + " not affected (SHA-256-Digest not found)");
                                        break;
                                    default:
                                        System.out.println("[INFO] Cleared META-INF/MANIFEST.MF: removed " + size + " lines");
                                        break;
                                }
                            } catch (Throwable throwable) {
                                System.out.println("[ERROR] Error on handle " + jarPath.getFileName() + " file...");
                                throwable.printStackTrace();
                            }
                        } catch (IOException e) {
                            System.out.println("[WARN] Cant copy " + jarPath.getFileName() + " file to output directory, skip");
                            try {
                                Files.delete(result);
                            } catch (Throwable ignored) {}
                            e.printStackTrace();
                        }
                    });
            System.out.println("[INFO] Done (" + (System.currentTimeMillis() - startTime) + " ms)!");
        } catch (Throwable t) {
            System.out.println("[ERROR] Cant create output directory or another error!");
            t.printStackTrace();
        }
    }

    private static int clearManifest(Path manifest) {
        if(Files.exists(manifest)) {
            ArrayList<String> mfResult = new ArrayList<>();
            int removed = 0;
            try {
                for (String str : Files.readAllLines(manifest)) {
                    if(str.isEmpty()) continue;
                    if(!str.startsWith("Name: ") && !str.startsWith("SHA-256-Digest: ") && !str.startsWith(" "))
                        mfResult.add(str);
                    else removed++;
                }
            } catch (Throwable t) {
                System.out.println("[ERROR] Cant read manifest lines from " + manifest);
                t.printStackTrace();
                return -1;
            }
            try {
                Files.delete(manifest);
                Files.write(manifest, mfResult, StandardOpenOption.CREATE);
            } catch (Throwable t) {
                System.out.println("[ERROR] Cant rewrite new manifest for " + manifest);
                t.printStackTrace();
                return -1;
            }
            return removed;
        } else return -2;
    }
}

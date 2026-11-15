package services;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BackendManeger {
    private static Process pythonProcess;

    public static void startBackend() {
        try{
            String pythonPath, scriptDir, os, progectDir;
            progectDir = System.getProperty("user.dir");
            os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                pythonPath = Path.of(progectDir, "venv", "Scripts", "python.exe").toString();
            } else {
                pythonPath = Path.of(progectDir, "venv", "bin", "python3").toString();
            }

            scriptDir = Paths.get(progectDir, "src", "backend", "python").toString();

            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonPath, "main.py"
            );

            processBuilder.directory(new File(scriptDir));
            processBuilder.redirectErrorStream(true);

            pythonProcess = processBuilder.start();
            System.out.println("System > Back-end Python iniciado");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (pythonProcess != null && pythonProcess.isAlive()) {
                    pythonProcess.destroy();
                    System.out.println("System > Back-end Python encerrado");
                }
            }));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

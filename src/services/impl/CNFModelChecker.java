package services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import repositories.model.ModelCheckResult;
import services.CNFModelCheckerService;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CNFModelChecker {
    static boolean computeBackwards;
    static String configPath;

    public static void main(String[] args) {
        configPath = args[0];
        computeBackwards = Boolean.parseBoolean(args[1]);

        new CNFModelChecker().run();
    }

    public void run() {
        CNFModelCheckerService cnfModelCheckerService = new CNFModelCheckerServiceImpl();
        List<ModelCheckResult> results = cnfModelCheckerService.getModelCheckingResult(configPath, computeBackwards);

//        System.out.println("model checking results: ");
//
//        for (ModelCheckResult res :
//                results) {
//            System.out.println(res.toString());
//        }
        System.out.println("great, now writing results to json: ");

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(new File("resources/results.json"), results);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

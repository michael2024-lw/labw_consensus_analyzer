package utils;

import exceptions.IncorrectCNFException;
import repositories.model.CNF.CNFModel;
import repositories.model.CNF.LiteralModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModelParser {
    public CNFModel parseCNFModel(File configFile) throws IOException, IncorrectCNFException {
        CNFModel cnf = new CNFModel();

        BufferedReader lineReader = new BufferedReader(new FileReader(configFile));
        String lineText;

        while ((lineText = lineReader.readLine()) != null) {
            if (lineText.startsWith("    Name:")) {
                cnf.addOrganization(lineText.substring(10));
            }

            if (lineText.startsWith("    Rule:")) {
                String realLiterals = lineText.substring(7, lineText.length() - 1);
                StringBuilder str = new StringBuilder();

                for (int i = 1; i < realLiterals.length(); ++i) {
                    if (realLiterals.charAt(i - 1) == '(') {
                        while (realLiterals.charAt(i) != ')') {
                            str.append(realLiterals.charAt(i));
                            ++i;
                        }


                        // TODO: in literals organizations must go in the same order as in the specification, even if the cnf formula it's specified in different order
                        //TODO: For example: org1, org2; CNF: (org2 AND org1), then CNFModel must be with 1 literal with list containing at the first index org1, and second org2
                        final Set<String> setToReturn = new LinkedHashSet<>();
                        final Set<String> tempSet = new HashSet<>();

                        for (String tempStr : str.toString().split(" AND ")) {
                            String realLiteral = tempStr;
                            if (tempStr.contains(" ")) {
                                tempStr = tempStr.substring(tempStr.lastIndexOf(" ") + 1);
                            }

                            if (tempSet.add(tempStr)) {
                                setToReturn.add(realLiteral);
                            } else {
                                throw new IncorrectCNFException("Invalid CNF formula");
                            }
                        }

                        LiteralModel literals = new LiteralModel(new ArrayList<>(tempSet));
                        literals.getLiteralMembers().sort(Comparator.comparing(item -> cnf.getOrganizations().indexOf(item)));

                        for (int j = 0; j < literals.getLiteralMembers().size(); j++) {
                            if (!setToReturn.contains(literals.getLiteralMembers().get(j))) {
                                StringBuilder tempStr = new StringBuilder(literals.getLiteralMembers().get(j));
                                literals.setLiteral(tempStr.insert(0, "NOT ").toString(), j);
                            }
                        }

                        cnf.addModel(literals);
                        str.setLength(0);
                    }
                }
            }
        }

        return cnf;
    }

    public Map<String, Double> parseAcceptanceProbabilities(File configFile) throws IOException, IncorrectCNFException {
        Map<String, Double> orgProbabilities = new HashMap<>();
        BufferedReader lineReader = new BufferedReader(new FileReader(configFile));
        String lineText;
        String org = null;
        double probability;

        while ((lineText = lineReader.readLine()) != null) {
            if (lineText.startsWith("    Name:")) {
                org = lineText.substring(10);
            }

            if (lineText.startsWith("    Pr:")) {
                probability = Double.parseDouble(lineText.substring(8));
                orgProbabilities.put(org, probability);
            }
        }

        StringBuilder errorMessage = new StringBuilder();

        for (Map.Entry<String, Double> orgProbs : orgProbabilities.entrySet()) {
            if (orgProbs.getValue() > 1) {
                if (!errorMessage.toString().equals("")) {
                    errorMessage.append(", ");
                }

                errorMessage.append("probability of ").append(orgProbs.getKey()).append(" is greater than one");
            } else if (orgProbs.getValue() < 0) {
                if (!errorMessage.toString().equals("")) {
                    errorMessage.append(",");
                }

                errorMessage.append("probability of ").append(orgProbs.getKey()).append(" is less than zero");
            }
        }

        if (!errorMessage.toString().equals("")) {
            throw new IncorrectCNFException(errorMessage.toString());
        }

        return orgProbabilities;
    }

    public List<int[]> parseBackwardTransitions(File configFile, double probability, double expectedMessages){
        return new ArrayList<>();
    }

    public List<int[]> parseSpecificationToBinary(CNFModel cnfModel){
        return new ArrayList<>();
    }
}

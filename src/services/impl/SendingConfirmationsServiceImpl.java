package services.impl;

import exceptions.IncorrectCNFException;
import repositories.model.CNF.CNFModel;
import services.EPVerifier;
import services.SendingConfirmationsService;
import utils.ModelParser;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class SendingConfirmationsServiceImpl implements SendingConfirmationsService {
    List<String> cashedOrganizations;

    @Override
    public boolean sendForConfirmation(EPVerifier epVerifier, String configPath, String analyticsPath,
                                       double probability, double expectedMessages) {
        File configFile = new File(configPath);
        File analyticsFile = new File(analyticsPath);
        ModelParser modelParser = new ModelParser();

        try {
            CNFModel cnfModel = modelParser.parseCNFModel(configFile);

            this.cashedOrganizations = cnfModel.getOrganizations();
            int[] responses = IntStream.generate(() -> -1)
                    .limit(cashedOrganizations.size())
                    .toArray();

            List<int[]> backwards = modelParser.parseBackwardTransitions(analyticsFile, probability, expectedMessages);
            List<int[]> specs = modelParser.parseSpecificationToBinary(cnfModel);

            return sendForConfirmationToOrganization(epVerifier, cashedOrganizations, responses, 0, specs, backwards);
        } catch (IOException | IncorrectCNFException e) {
            e.printStackTrace();
        }

        // in case of error
        return false;
    }

    private boolean sendForConfirmationToOrganization(EPVerifier epVerifier, List<String> organizations,
                                                      int[] responses, int cnt, List<int[]> spec, List<int[]> backwards) {
        int reply = epVerifier.getReply(organizations.get(0), 0);
        responses[cnt] = reply;

        Optional<int[]> specOpt = spec.stream().filter(s -> Arrays.equals(responses, s)).findAny();
        Optional<int[]> backwardsOpt = backwards.stream().filter(b -> Arrays.equals(responses, b)).findAny();

        if (specOpt.isPresent()) {
            return true;
        } else if (backwardsOpt.isPresent()) {
            int[] initialResponses = IntStream.generate(() -> -1)
                    .limit(cashedOrganizations.size())
                    .toArray();

            sendForConfirmationToOrganization(epVerifier, this.cashedOrganizations, initialResponses, 0, spec,
                    backwards);
        } else if (IntStream.of(responses).noneMatch(x -> x == -1)) {
            return false;
        } else {
            sendForConfirmationToOrganization(epVerifier, organizations, responses, cnt + 1, spec, backwards);
        }

        // should never happen
        return false;
    }
}

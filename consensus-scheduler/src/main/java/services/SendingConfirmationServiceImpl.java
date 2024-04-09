package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import repositories.model.ModelCheckResult;
import repositories.model.ModelCheckerResultWrapper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.IntStream;

public class SendingConfirmationServiceImpl implements SendingConfirmationService {
    List<String> cashedOrganizations;

    Logger logger = Logger.getLogger("SchedulerLog");
    FileHandler fh;


    public SendingConfirmationServiceImpl() {

    }

    @Override
    public boolean sendForConfirmationCustomValues(SenderService senderService, String analyticsPath,
                                                   double probability, double messages) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            ModelCheckerResultWrapper modelCheckerResultWrapper = objectMapper.readValue(new File(analyticsPath),
                    ModelCheckerResultWrapper.class);

            // TODO: if such element is not present, find the closest one
            ModelCheckResult result = modelCheckerResultWrapper.getModelCheckResultList().stream()
                    .filter(r -> r.getProbability() == probability && r.getExpectedMessages() == messages)
                    .findAny()
                    .orElseThrow();

            return sendForConfirmation(senderService, result, modelCheckerResultWrapper, senderService.getLogPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // in case of error
        return false;
    }

    @Override
    public boolean sendForConfirmationCustomId(SenderService senderService, String analyticsPath, int modelId) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            ModelCheckerResultWrapper modelCheckerResultWrapper = objectMapper.readValue(new File(analyticsPath),
                    ModelCheckerResultWrapper.class);

            ModelCheckResult result = modelCheckerResultWrapper.getModelCheckResultList().stream()
                    .filter(r -> r.getId() == modelId)
                    .findAny()
                    .orElseThrow();

            return sendForConfirmation(senderService, result, modelCheckerResultWrapper, senderService.getLogPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // in case of error
        return false;
    }

    @Override
    public boolean sendForConfirmationMinMessages(SenderService senderService, String analyticsPath) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            ModelCheckerResultWrapper modelCheckerResultWrapper = objectMapper.readValue(new File(analyticsPath),
                    ModelCheckerResultWrapper.class);

            // TODO: if multiple results, then get with max probability
            ModelCheckResult result = modelCheckerResultWrapper.getModelCheckResultList().stream()
                    .min(Comparator.comparing(ModelCheckResult::getExpectedMessages))
                    .orElseThrow();

            return sendForConfirmation(senderService, result, modelCheckerResultWrapper, senderService.getLogPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // in case of error
        return false;
    }

    @Override
    public boolean sendForConfirmationMaxProbability(SenderService senderService, String analyticsPath) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            ModelCheckerResultWrapper modelCheckerResultWrapper = objectMapper.readValue(new File(analyticsPath),
                    ModelCheckerResultWrapper.class);

            // TODO: if multiple results, then get with min messages
            ModelCheckResult result = modelCheckerResultWrapper.getModelCheckResultList().stream()
                    .max(Comparator.comparing(ModelCheckResult::getProbability))
                    .orElseThrow();

            return sendForConfirmation(senderService, result, modelCheckerResultWrapper, senderService.getLogPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // in case of error
        return false;
    }

    private boolean sendForConfirmation(SenderService senderService, ModelCheckResult result,
                                        ModelCheckerResultWrapper modelCheckerResultWrapper, String logPath)
            throws IOException {
        fh = new FileHandler(logPath);
        logger.addHandler(fh);
        logger.setUseParentHandlers(false);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);

        this.cashedOrganizations = modelCheckerResultWrapper.getOrganizations();
        int[] responses = IntStream.generate(() -> -1)
                .limit(cashedOrganizations.size())
                .toArray();

        Set<int[]> backwards = result.getBackwardTransitions();
        Set<int[]> specs = modelCheckerResultWrapper.getSpecification();

        Map<String, Integer> numberOfRequestsPerOrganization = new HashMap<>();
        cashedOrganizations.forEach(o -> numberOfRequestsPerOrganization.put(o, 0));

        return sendForConfirmationToOrganization(senderService, cashedOrganizations, responses, 0,
                0, specs, backwards, numberOfRequestsPerOrganization);
    }

    private boolean sendForConfirmationToOrganization(SenderService senderService, List<String> organizations,
                                                      int[] responses, int responseIndex, int messagesCnt,
                                                      Set<int[]> spec, Set<int[]> backwards,
                                                      Map<String, Integer> numberOfRequestsPerOrganization) {
        String orgToSend = organizations.get(0);

        int requestsNumToOrg = numberOfRequestsPerOrganization.get(orgToSend);

        numberOfRequestsPerOrganization.put(orgToSend, requestsNumToOrg + 1);
        int reply = senderService.getReply(orgToSend, 0);
        logger.info(String.format("Send for confirmation to %s finished with response %b", orgToSend, reply == 1));

        responses[responseIndex] = reply;

        Optional<int[]> specOpt = spec.stream().filter(s -> Arrays.equals(responses, s)).findAny();
        Optional<int[]> backwardsOpt = backwards.stream().filter(b -> Arrays.equals(responses, b)).findAny();

        if (backwardsOpt.isPresent()) {
            if (requestsNumToOrg >= senderService.getMaxRequestNum()) {
                logger.warning(String.format("Organization %s has reached the max amount of requests. " +
                        "Remove backward transition for this organization.", orgToSend));
                backwards.remove(responses);
            }

            backwardsOpt = backwards.stream().filter(b -> Arrays.equals(responses, b)).findAny();
        }

        if (specOpt.isPresent()) {
            logger.info(String.format("Consensus is reached with %d messages", messagesCnt + 1));
            return true;
        } else if (messagesCnt == senderService.getMaxRequestTotalNum()) {
            logger.warning("Max number of messages was sent. Consensus is not reached.");
            return false;
        } else if (backwardsOpt.isPresent() && reply != 1) {
            int[] initialResponses = IntStream.generate(() -> -1)
                    .limit(cashedOrganizations.size())
                    .toArray();

            logger.info(String.format("Make backward transition from organization %s", orgToSend));

            return sendForConfirmationToOrganization(senderService, this.cashedOrganizations, initialResponses, 0,
                    messagesCnt + 1, spec, backwards, numberOfRequestsPerOrganization);
        } else if (IntStream.of(responses).noneMatch(x -> x == -1)) {
            // reached the end of the tree
            return false;
        } else {
            organizations.remove(orgToSend);
            return sendForConfirmationToOrganization(senderService, organizations, responses, responseIndex + 1,
                    messagesCnt + 1, spec, backwards, numberOfRequestsPerOrganization);
        }
    }
}

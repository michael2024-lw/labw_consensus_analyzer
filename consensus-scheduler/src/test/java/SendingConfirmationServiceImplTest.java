import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import services.SenderService;
import services.SendingConfirmationService;
import services.SendingConfirmationServiceImpl;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SendingConfirmationServiceImplTest {

    static SendingConfirmationService confirmationService;


    public SendingConfirmationServiceImplTest() {

    }

    @BeforeAll
    static void init() {
        confirmationService = new SendingConfirmationServiceImpl();
    }

    @Test
    public void sendCustomIdTest() {
        boolean consensusReached = confirmationService.sendForConfirmationCustomId(new SenderServiceImpl(),
                "src/test/resources/results.json",
                0);

        assertTrue(consensusReached);
    }

    @Test
    public void sendCustomValuesTest() {
        boolean consensusReached = confirmationService.sendForConfirmationCustomValues(new SenderServiceImpl(),
                "src/test/resources/results.json",
                1.0,
                3.0919090686339907);

        assertTrue(consensusReached);
    }

    @Test
    public void sendMinMessagesTest() {
        boolean consensusReached = confirmationService.sendForConfirmationMinMessages(new SenderServiceImpl(),
                "src/test/resources/results.json");

        assertTrue(consensusReached);
    }

    @Test
    public void sendMaxProbabilityTest() {
        boolean consensusReached = confirmationService.sendForConfirmationMaxProbability(new SenderServiceImpl(),
                "src/test/resources/results.json");

        assertTrue(consensusReached);
    }

    static class SenderServiceImpl implements SenderService {
        @Override
        public int getReply(String orgName, int transactionId) {
            // random either accepts or rejects a transaction
	    switch(orgName) {
		case "Org1": // 0.995
		    return (ThreadLocalRandom.current().nextInt(0, 200) == 0 ? 0 : 1);
		case "Org2": // 0.99
		    return (ThreadLocalRandom.current().nextInt(0, 100) == 0 ? 0 : 1);
		case "Org3": // 0.985
		    return (ThreadLocalRandom.current().nextInt(0, 67) == 0 ? 0 : 1);
		case "Org4": // 0.9925
                    return (ThreadLocalRandom.current().nextInt(0, 133) == 0 ? 0 : 1);
	    }
            return ThreadLocalRandom.current().nextInt(0, 2);
        }

        @Override
        public int getMaxRequestNum() {
            return 10;
        }

        @Override
        public int getMaxRequestTotalNum() {
            return 50;
        }

        @Override
        public long getTimeoutSec() {
            return 0;
        }

        @Override
        public long getWaitingTimeSec() {
            return 0;
        }

        @Override
        public String getLogPath() {
            return "src/test/resources/runTest.log";
        }
    }
}

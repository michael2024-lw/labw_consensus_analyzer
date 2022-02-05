package services;

public interface SendingConfirmationsService {
    boolean sendForConfirmation(EPVerifier epVerifier, String configPath, String analyticsPath,
                                double probability, double expectedMessages);
}



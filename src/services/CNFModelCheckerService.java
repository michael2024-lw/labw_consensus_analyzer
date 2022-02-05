package services;

import repositories.model.ModelCheckResult;

import java.util.List;

public interface CNFModelCheckerService {
    List<ModelCheckResult> getModelCheckingResult(String configPath, boolean computeBackwards);
}

package com.cinx.reporting.analytics.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.reporting.analytics.application.command.CreateExperimentCommand;
import com.cinx.reporting.analytics.application.query.AssignmentView;
import com.cinx.reporting.analytics.application.query.ExperimentResultsView;
import com.cinx.reporting.analytics.application.query.ExperimentResultsView.VariantResult;
import com.cinx.reporting.analytics.application.query.ExperimentView;
import com.cinx.reporting.analytics.domain.Experiment;
import com.cinx.reporting.analytics.domain.ExperimentRepository;
import com.cinx.reporting.analytics.domain.InvalidProductEventException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExperimentService implements IExperimentService {
    private final ExperimentRepository experiments;
    private final AnalyticsQueries queries;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentView> assignments(String userId) {
        return experiments.findRunning().stream()
                .map(experiment -> new AssignmentView(experiment.getKey(), experiment.assign(userId)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExperimentView> list() {
        return experiments.findAll().stream()
                .sorted(Comparator.comparing(Experiment::getCreatedAt).reversed())
                .map(ExperimentView::of)
                .toList();
    }

    @Override
    @Transactional
    public ExperimentView create(CreateExperimentCommand command, String by) {
        if (experiments.findByKey(command.key()).isPresent()) {
            throw new ConflictException(ErrorCode.RESOURCE_ALREADY_EXISTS, "An experiment with this key exists");
        }
        Experiment draft = rules(() -> Experiment.draft(command.key(), command.description(),
                command.variants() == null ? null : command.variants().stream()
                        .map(v -> new Experiment.Variant(v.key(), v.weight())).toList(),
                command.goalEvent(), command.goalFilter(), command.goalWindowDays(),
                UUID.randomUUID().toString(), by, now()));
        return ExperimentView.of(experiments.save(draft));
    }

    @Override
    @Transactional
    public ExperimentView start(String key, String by) {
        Experiment experiment = find(key);
        return ExperimentView.of(experiments.save(rules(() -> experiment.start(by, now()))));
    }

    @Override
    @Transactional
    public ExperimentView stop(String key, String by) {
        Experiment experiment = find(key);
        return ExperimentView.of(experiments.save(rules(() -> experiment.stop(by, now()))));
    }

    @Override
    @Transactional(readOnly = true)
    public ExperimentResultsView results(String key) {
        Experiment experiment = find(key);
        Map<String, VariantResult> counted = queries.experimentResults(key, experiment.getGoalEvent(),
                        experiment.getGoalFilter(), experiment.getGoalWindowDays()).stream()
                .collect(Collectors.toMap(VariantResult::variant, Function.identity()));
        // every variant is listed, with zeros before anyone reached it
        List<VariantResult> variants = experiment.getVariants().stream()
                .map(v -> counted.getOrDefault(v.key(), new VariantResult(v.key(), 0, 0)))
                .toList();
        return new ExperimentResultsView(ExperimentView.of(experiment), variants);
    }

    private Experiment find(String key) {
        return experiments.findByKey(key).orElseThrow(() -> new NotFoundException("No experiment " + key));
    }

    private static Experiment rules(Supplier<Experiment> change) {
        try {
            return change.get();
        } catch (InvalidProductEventException broken) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, broken.getMessage());
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}

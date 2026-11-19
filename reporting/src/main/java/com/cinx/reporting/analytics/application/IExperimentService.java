package com.cinx.reporting.analytics.application;

import com.cinx.reporting.analytics.application.command.CreateExperimentCommand;
import com.cinx.reporting.analytics.application.query.AssignmentView;
import com.cinx.reporting.analytics.application.query.ExperimentResultsView;
import com.cinx.reporting.analytics.application.query.ExperimentView;

import java.util.List;

public interface IExperimentService {
    /** The user's variant of every running experiment; anything not listed is control. */
    List<AssignmentView> assignments(String userId);

    List<ExperimentView> list();

    ExperimentView create(CreateExperimentCommand command, String by);

    ExperimentView start(String key, String by);

    ExperimentView stop(String key, String by);

    ExperimentResultsView results(String key);
}

package com.cinx.learning.dailyquiz.application;

import com.cinx.learning.dailyquiz.application.query.DailyQuizView;
import com.cinx.learning.dailyquiz.application.command.UpsertDailyQuizCommand;
import com.cinx.learning.featured.application.command.ReorderCommand;

import java.util.List;

public interface IDailyQuizService {
    /** Today's question for guests: active questions rotate by day in sort order. */
    DailyQuizView getToday();

    List<DailyQuizView> getAll();
    DailyQuizView create(UpsertDailyQuizCommand request);
    DailyQuizView update(String id, UpsertDailyQuizCommand request);
    void delete(String id);
    List<DailyQuizView> reorder(ReorderCommand request);
}

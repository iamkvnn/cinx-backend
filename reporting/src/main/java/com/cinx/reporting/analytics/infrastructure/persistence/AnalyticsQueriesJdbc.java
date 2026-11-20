package com.cinx.reporting.analytics.infrastructure.persistence;

import com.cinx.reporting.analytics.application.AnalyticsQueries;
import com.cinx.reporting.analytics.application.query.ExperimentResultsView.VariantResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
class AnalyticsQueriesJdbc implements AnalyticsQueries {
    // a goal filter prop becomes part of a JSON path, so only plain prop names get that far
    private static final Pattern PROP = Pattern.compile("[A-Za-z][A-Za-z0-9]{0,63}");

    private final JdbcTemplate jdbc;

    @Override
    public List<VariantResult> experimentResults(String experimentKey, String goalEvent, Map<String, String> goalFilter, int windowDays) {
        StringBuilder goal = new StringBuilder("""
                SELECT 1 FROM product_event e
                WHERE e.user_id = x.user_id AND e.name = ?
                  AND e.occurred_at >= x.exposed_at AND e.occurred_at < x.exposed_at + INTERVAL ? DAY""");
        List<Object> args = new ArrayList<>(List.of(goalEvent, windowDays));
        goalFilter.forEach((prop, value) -> {
            if (!PROP.matcher(prop).matches()) throw new IllegalArgumentException("Not a prop name: " + prop);
            goal.append(" AND JSON_UNQUOTE(JSON_EXTRACT(e.props, '$.").append(prop).append("')) = ?");
            args.add(value);
        });
        args.add(experimentKey);
        return jdbc.query("SELECT x.variant, COUNT(*) AS exposed, SUM(EXISTS (" + goal + ")) AS converted "
                        + "FROM experiment_exposure x WHERE x.experiment_key = ? GROUP BY x.variant",
                (rs, row) -> new VariantResult(rs.getString("variant"), rs.getLong("exposed"), rs.getLong("converted")),
                args.toArray());
    }

    @Override
    public List<Long> funnel(List<String> steps, LocalDateTime from, LocalDateTime to) {
        StringBuilder sql = new StringBuilder("WITH s1 AS (SELECT user_id, MIN(occurred_at) AS t FROM product_event "
                + "WHERE name = ? AND occurred_at >= ? AND occurred_at < ? GROUP BY user_id)");
        List<Object> args = new ArrayList<>(List.of(steps.get(0), Timestamp.valueOf(from), Timestamp.valueOf(to)));
        for (int i = 2; i <= steps.size(); i++) {
            sql.append(", s").append(i).append(" AS (SELECT e.user_id, MIN(e.occurred_at) AS t FROM product_event e JOIN s")
                    .append(i - 1).append(" p ON p.user_id = e.user_id AND e.occurred_at >= p.t ")
                    .append("WHERE e.name = ? AND e.occurred_at < ? GROUP BY e.user_id)");
            args.add(steps.get(i - 1));
            args.add(Timestamp.valueOf(to));
        }
        sql.append(" SELECT ");
        for (int i = 1; i <= steps.size(); i++) {
            sql.append(i > 1 ? ", " : "").append("(SELECT COUNT(*) FROM s").append(i).append(")");
        }
        return jdbc.queryForObject(sql.toString(), (rs, row) -> {
            List<Long> users = new ArrayList<>();
            for (int i = 1; i <= steps.size(); i++) users.add(rs.getLong(i));
            return users;
        }, args.toArray());
    }
}

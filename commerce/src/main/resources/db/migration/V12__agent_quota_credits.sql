-- AI assistant usage is metered in credits (1 credit = 1,000 weighted tokens) instead of requests.
-- Registry and plan grants change together: PlanCatalogService rejects a grant whose unit differs from the registry.
UPDATE commerce_feature_registry
   SET unit = 'CREDIT'
 WHERE feature_key = 'recommendation.agent.requests';

UPDATE plan_entitlement
   SET quota_unit = 'CREDIT'
 WHERE feature_key = 'recommendation.agent.requests';

-- Learner Free: 350 credits a month, about ten learning-path conversations.
UPDATE plan_entitlement
   SET quota_limit = 350
 WHERE id = '00000000-0000-0000-0000-000000000113';

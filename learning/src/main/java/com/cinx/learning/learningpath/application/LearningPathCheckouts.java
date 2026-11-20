package com.cinx.learning.learningpath.application;

/** Port: asks the commerce side to put a course into the learner's cart, as an event in the caller's transaction. */
public interface LearningPathCheckouts {
    void requestCheckout(String userId, String courseId);
}

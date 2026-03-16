package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.ActivityPollVote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityPollVoteRepository extends JpaRepository<ActivityPollVote, Long> {

    Optional<ActivityPollVote> findByPollIdAndVoterId(Long pollId, Long voterId);

    @EntityGraph(attributePaths = {"poll", "option", "voter"})
    List<ActivityPollVote> findByPollIdInOrderByVotedAtAsc(List<Long> pollIds);

    long deleteByOptionId(Long optionId);

    long deleteByPollId(Long pollId);

    long deleteByPollActivityId(Long activityId);
}

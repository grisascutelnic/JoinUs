package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.ActivityPollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityPollOptionRepository extends JpaRepository<ActivityPollOption, Long> {

    List<ActivityPollOption> findByPollIdOrderByPositionAsc(Long pollId);

    List<ActivityPollOption> findByPollIdInOrderByPollIdAscPositionAsc(List<Long> pollIds);

    Optional<ActivityPollOption> findByIdAndPollId(Long optionId, Long pollId);

    List<ActivityPollOption> findByPollId(Long pollId);

    long deleteByPollId(Long pollId);

    long deleteByPollActivityId(Long activityId);
}

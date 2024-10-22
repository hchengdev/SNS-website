package com.snsapi.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    List<User> findByNameContainingIgnoreCase(@Param("name") String name);

    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u JOIN ChatMessage cm ON (cm.sender.id = u.id OR cm.receiver.id = u.id) WHERE u.id <> ?1")
    List<User> findConversationsByUserId(Integer userId);
    @Query("SELECT u FROM User u JOIN u.sentMessages m WHERE m.receiver.id = :userId " +
            "OR m.sender.id = :userId")
    List<User> findFriendsByUserId(@Param("userId") Integer userId);
    // number of user by month of a year
    @Query(value = "WITH Months AS (SELECT 1 AS month UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12) SELECT Months.month, coalesce(u.newUser, 0) as newUsers FROM Months LEFT JOIN  (SELECT MONTH(creation_date) AS `month`, COUNT(*) AS newUser FROM users WHERE YEAR(creation_date) = :year GROUP BY MONTH(creation_date)) AS u ON u.month = Months.month", nativeQuery = true)
    List<NewUserByMonthResponse> getUserNumberByMonthOfYear(@Param("year") int year);

    // number of user by week of a month in a year
//    @Query(value = "SELECT WEEK(creation_date) as inWeek, COUNT(*) as 'NumberOfUsers' FROM users WHERE YEAR(creation_date) =:year AND MONT(creation_date) = :month GROUP BY WEEK(creation_date)", nativeQuery = true)
//    List<User> getUserNumberByWeekOfMonthOfYear(@Param("month") int month, @Param("year") int year);

    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%' ,:name, '%'))")
    List<User> findByName(@Param("name") String name);

}


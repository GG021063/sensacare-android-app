package com.sensacare.app.data.local.dao

import androidx.room.*
import com.sensacare.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * AlertDao - Data Access Object for health alerts and emergency management
 *
 * This DAO provides comprehensive access to health alerts, alert rules, and emergency
 * contacts, including:
 * - Alert CRUD operations
 * - Alert rule management and evaluation
 * - Emergency contact management
 * - Alert history and notification tracking
 * - Smart alert filtering and prioritization
 * - Alert escalation and response management
 * - Clinical threshold monitoring
 *
 * The AlertDao enables the app to monitor health metrics, detect anomalies,
 * generate appropriate alerts, and manage emergency responses.
 */
@Dao
interface AlertDao {

    /**
     * Alert CRUD Operations
     */

    /**
     * Insert a single health alert
     * @param alert The health alert entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: HealthAlertEntity): Long

    /**
     * Insert multiple health alerts in a single transaction
     * @param alerts List of health alert entities to insert
     * @return List of row IDs for the inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alerts: List<HealthAlertEntity>): List<Long>

    /**
     * Update a health alert
     * @param alert The health alert entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun update(alert: HealthAlertEntity): Int

    /**
     * Delete a health alert
     * @param alert The health alert entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun delete(alert: HealthAlertEntity): Int

    /**
     * Delete a health alert by ID
     * @param alertId The ID of the health alert to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM health_alerts WHERE id = :alertId")
    suspend fun deleteById(alertId: String): Int

    /**
     * Delete all alerts for a specific user
     * @param userId The user ID
     * @return Number of rows deleted
     */
    @Query("DELETE FROM health_alerts WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String): Int

    /**
     * Delete all resolved alerts for a specific user
     * @param userId The user ID
     * @param olderThan Optional date to delete alerts older than
     * @return Number of rows deleted
     */
    @Query("""
        DELETE FROM health_alerts 
        WHERE userId = :userId 
        AND status = 'RESOLVED' 
        AND (:olderThan IS NULL OR timestamp < :olderThan)
    """)
    suspend fun deleteAllResolvedAlerts(
        userId: String,
        olderThan: LocalDateTime? = null
    ): Int

    /**
     * Basic Queries for Alerts
     */

    /**
     * Get health alert by ID
     * @param alertId The ID of the health alert to retrieve
     * @return The health alert entity or null if not found
     */
    @Query("SELECT * FROM health_alerts WHERE id = :alertId")
    suspend fun getById(alertId: String): HealthAlertEntity?

    /**
     * Get health alert by ID as Flow for reactive updates
     * @param alertId The ID of the health alert to retrieve
     * @return Flow emitting the health alert entity
     */
    @Query("SELECT * FROM health_alerts WHERE id = :alertId")
    fun getByIdAsFlow(alertId: String): Flow<HealthAlertEntity?>

    /**
     * Get all health alerts for a specific user
     * @param userId The user ID
     * @param limit Maximum number of alerts to return
     * @return List of health alert entities
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getAllForUser(userId: String, limit: Int = 100): List<HealthAlertEntity>

    /**
     * Get all health alerts for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @param limit Maximum number of alerts to return
     * @return Flow emitting list of health alert entities
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    fun getAllForUserAsFlow(userId: String, limit: Int = 100): Flow<List<HealthAlertEntity>>

    /**
     * Get alerts by metric type
     * @param userId The user ID
     * @param metricType The metric type to filter by
     * @param limit Maximum number of alerts to return
     * @return List of health alert entities for the specified metric type
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId AND metricType = :metricType 
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getByMetricType(
        userId: String,
        metricType: String,
        limit: Int = 50
    ): List<HealthAlertEntity>

    /**
     * Get alerts by severity
     * @param userId The user ID
     * @param severity The severity level to filter by
     * @param limit Maximum number of alerts to return
     * @return List of health alert entities for the specified severity
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId AND severity = :severity 
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getBySeverity(
        userId: String,
        severity: String,
        limit: Int = 50
    ): List<HealthAlertEntity>

    /**
     * Get alerts by status
     * @param userId The user ID
     * @param status The status to filter by
     * @param limit Maximum number of alerts to return
     * @return List of health alert entities for the specified status
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId AND status = :status 
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getByStatus(
        userId: String,
        status: String,
        limit: Int = 50
    ): List<HealthAlertEntity>

    /**
     * Get alerts by time range
     * @param userId The user ID
     * @param startTime Start time of the range
     * @param endTime End time of the range
     * @param limit Maximum number of alerts to return
     * @return List of health alert entities within the specified time range
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        AND timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getByTimeRange(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        limit: Int = 100
    ): List<HealthAlertEntity>

    /**
     * Alert Rule Management and Evaluation
     */

    /**
     * Insert a single alert rule
     * @param rule The alert rule entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AlertRuleEntity): Long

    /**
     * Insert multiple alert rules in a single transaction
     * @param rules List of alert rule entities to insert
     * @return List of row IDs for the inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRules(rules: List<AlertRuleEntity>): List<Long>

    /**
     * Update an alert rule
     * @param rule The alert rule entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun updateRule(rule: AlertRuleEntity): Int

    /**
     * Delete an alert rule
     * @param rule The alert rule entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun deleteRule(rule: AlertRuleEntity): Int

    /**
     * Delete an alert rule by ID
     * @param ruleId The ID of the alert rule to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM alert_rules WHERE id = :ruleId")
    suspend fun deleteRuleById(ruleId: String): Int

    /**
     * Get alert rule by ID
     * @param ruleId The ID of the alert rule to retrieve
     * @return The alert rule entity or null if not found
     */
    @Query("SELECT * FROM alert_rules WHERE id = :ruleId")
    suspend fun getRuleById(ruleId: String): AlertRuleEntity?

    /**
     * Get alert rule by ID as Flow for reactive updates
     * @param ruleId The ID of the alert rule to retrieve
     * @return Flow emitting the alert rule entity
     */
    @Query("SELECT * FROM alert_rules WHERE id = :ruleId")
    fun getRuleByIdAsFlow(ruleId: String): Flow<AlertRuleEntity?>

    /**
     * Get all alert rules for a specific user
     * @param userId The user ID
     * @return List of alert rule entities
     */
    @Query("SELECT * FROM alert_rules WHERE userId = :userId ORDER BY priority DESC")
    suspend fun getAllRulesForUser(userId: String): List<AlertRuleEntity>

    /**
     * Get all alert rules for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of alert rule entities
     */
    @Query("SELECT * FROM alert_rules WHERE userId = :userId ORDER BY priority DESC")
    fun getAllRulesForUserAsFlow(userId: String): Flow<List<AlertRuleEntity>>

    /**
     * Get active alert rules for a specific user
     * @param userId The user ID
     * @return List of active alert rule entities
     */
    @Query("SELECT * FROM alert_rules WHERE userId = :userId AND isActive = 1 ORDER BY priority DESC")
    suspend fun getActiveRulesForUser(userId: String): List<AlertRuleEntity>

    /**
     * Get alert rules by metric type
     * @param userId The user ID
     * @param metricType The metric type to filter by
     * @return List of alert rule entities for the specified metric type
     */
    @Query("""
        SELECT * FROM alert_rules 
        WHERE userId = :userId AND metricType = :metricType 
        ORDER BY priority DESC
    """)
    suspend fun getRulesByMetricType(userId: String, metricType: String): List<AlertRuleEntity>

    /**
     * Get alert rules by severity
     * @param userId The user ID
     * @param severity The severity level to filter by
     * @return List of alert rule entities for the specified severity
     */
    @Query("""
        SELECT * FROM alert_rules 
        WHERE userId = :userId AND severity = :severity 
        ORDER BY priority DESC
    """)
    suspend fun getRulesBySeverity(userId: String, severity: String): List<AlertRuleEntity>

    /**
     * Activate an alert rule
     * @param ruleId The rule ID
     * @param timestamp The timestamp of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE alert_rules 
        SET 
            isActive = 1, 
            modifiedAt = :timestamp
        WHERE id = :ruleId
    """)
    suspend fun activateRule(
        ruleId: String,
        timestamp: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Deactivate an alert rule
     * @param ruleId The rule ID
     * @param timestamp The timestamp of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE alert_rules 
        SET 
            isActive = 0, 
            modifiedAt = :timestamp
        WHERE id = :ruleId
    """)
    suspend fun deactivateRule(
        ruleId: String,
        timestamp: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Get applicable rules for a metric value
     * @param userId The user ID
     * @param metricType The metric type
     * @param metricValue The metric value to evaluate
     * @return List of applicable alert rule entities
     */
    @Query("""
        SELECT * FROM alert_rules 
        WHERE userId = :userId 
        AND metricType = :metricType 
        AND isActive = 1
        AND (
            (conditionType = 'GREATER_THAN' AND :metricValue > thresholdValue) OR
            (conditionType = 'LESS_THAN' AND :metricValue < thresholdValue) OR
            (conditionType = 'EQUAL_TO' AND :metricValue = thresholdValue) OR
            (conditionType = 'GREATER_THAN_OR_EQUAL' AND :metricValue >= thresholdValue) OR
            (conditionType = 'LESS_THAN_OR_EQUAL' AND :metricValue <= thresholdValue) OR
            (conditionType = 'NOT_EQUAL_TO' AND :metricValue != thresholdValue) OR
            (conditionType = 'RANGE' AND :metricValue BETWEEN thresholdValue AND secondaryThresholdValue) OR
            (conditionType = 'OUTSIDE_RANGE' AND (:metricValue < thresholdValue OR :metricValue > secondaryThresholdValue))
        )
        ORDER BY priority DESC
    """)
    suspend fun getApplicableRules(
        userId: String,
        metricType: String,
        metricValue: Double
    ): List<AlertRuleEntity>

    /**
     * Get rule trigger count
     * @param ruleId The rule ID
     * @param startTime Start time of the range
     * @param endTime End time of the range
     * @return Number of times the rule has been triggered
     */
    @Query("""
        SELECT COUNT(*) FROM health_alerts
        WHERE ruleId = :ruleId
        AND timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getRuleTriggerCount(
        ruleId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Int

    /**
     * Get rule effectiveness
     * @param ruleId The rule ID
     * @return Rule effectiveness metrics
     */
    @Query("""
        WITH rule_stats AS (
            SELECT 
                COUNT(*) as totalTriggers,
                SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END) as resolvedAlerts,
                SUM(CASE WHEN status = 'FALSE_ALARM' THEN 1 ELSE 0 END) as falseAlarms,
                SUM(CASE WHEN status = 'ACKNOWLEDGED' THEN 1 ELSE 0 END) as acknowledgedAlerts,
                SUM(CASE WHEN status = 'ESCALATED' THEN 1 ELSE 0 END) as escalatedAlerts
            FROM health_alerts
            WHERE ruleId = :ruleId
        )
        SELECT 
            totalTriggers,
            resolvedAlerts,
            falseAlarms,
            acknowledgedAlerts,
            escalatedAlerts,
            CASE 
                WHEN totalTriggers > 0 THEN (falseAlarms * 100.0 / totalTriggers)
                ELSE 0
            END as falseAlarmRate,
            CASE 
                WHEN totalTriggers > 0 THEN ((resolvedAlerts + acknowledgedAlerts) * 100.0 / totalTriggers)
                ELSE 0
            END as actionableAlertRate
        FROM rule_stats
    """)
    suspend fun getRuleEffectiveness(ruleId: String): RuleEffectiveness?

    /**
     * Emergency Contact Management
     */

    /**
     * Insert a single emergency contact
     * @param contact The emergency contact entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity): Long

    /**
     * Insert multiple emergency contacts in a single transaction
     * @param contacts List of emergency contact entities to insert
     * @return List of row IDs for the inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllContacts(contacts: List<EmergencyContactEntity>): List<Long>

    /**
     * Update an emergency contact
     * @param contact The emergency contact entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun updateContact(contact: EmergencyContactEntity): Int

    /**
     * Delete an emergency contact
     * @param contact The emergency contact entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun deleteContact(contact: EmergencyContactEntity): Int

    /**
     * Delete an emergency contact by ID
     * @param contactId The ID of the emergency contact to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM emergency_contacts WHERE id = :contactId")
    suspend fun deleteContactById(contactId: String): Int

    /**
     * Get emergency contact by ID
     * @param contactId The ID of the emergency contact to retrieve
     * @return The emergency contact entity or null if not found
     */
    @Query("SELECT * FROM emergency_contacts WHERE id = :contactId")
    suspend fun getContactById(contactId: String): EmergencyContactEntity?

    /**
     * Get emergency contact by ID as Flow for reactive updates
     * @param contactId The ID of the emergency contact to retrieve
     * @return Flow emitting the emergency contact entity
     */
    @Query("SELECT * FROM emergency_contacts WHERE id = :contactId")
    fun getContactByIdAsFlow(contactId: String): Flow<EmergencyContactEntity?>

    /**
     * Get all emergency contacts for a specific user
     * @param userId The user ID
     * @return List of emergency contact entities
     */
    @Query("""
        SELECT * FROM emergency_contacts 
        WHERE userId = :userId 
        ORDER BY priority DESC, name ASC
    """)
    suspend fun getAllContactsForUser(userId: String): List<EmergencyContactEntity>

    /**
     * Get all emergency contacts for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of emergency contact entities
     */
    @Query("""
        SELECT * FROM emergency_contacts 
        WHERE userId = :userId 
        ORDER BY priority DESC, name ASC
    """)
    fun getAllContactsForUserAsFlow(userId: String): Flow<List<EmergencyContactEntity>>

    /**
     * Get primary emergency contacts
     * @param userId The user ID
     * @return List of primary emergency contact entities
     */
    @Query("""
        SELECT * FROM emergency_contacts 
        WHERE userId = :userId AND isPrimary = 1
        ORDER BY priority DESC, name ASC
    """)
    suspend fun getPrimaryContacts(userId: String): List<EmergencyContactEntity>

    /**
     * Get contacts by relationship
     * @param userId The user ID
     * @param relationship The relationship to filter by
     * @return List of emergency contact entities for the specified relationship
     */
    @Query("""
        SELECT * FROM emergency_contacts 
        WHERE userId = :userId AND relationship = :relationship
        ORDER BY priority DESC, name ASC
    """)
    suspend fun getContactsByRelationship(
        userId: String,
        relationship: String
    ): List<EmergencyContactEntity>

    /**
     * Get contacts by notification preference
     * @param userId The user ID
     * @param notificationPreference The notification preference to filter by
     * @return List of emergency contact entities for the specified notification preference
     */
    @Query("""
        SELECT * FROM emergency_contacts 
        WHERE userId = :userId AND notificationPreference = :notificationPreference
        ORDER BY priority DESC, name ASC
    """)
    suspend fun getContactsByNotificationPreference(
        userId: String,
        notificationPreference: String
    ): List<EmergencyContactEntity>

    /**
     * Set contact as primary
     * @param contactId The contact ID
     * @param timestamp The timestamp of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE emergency_contacts 
        SET 
            isPrimary = 1, 
            modifiedAt = :timestamp
        WHERE id = :contactId
    """)
    suspend fun setContactAsPrimary(
        contactId: String,
        timestamp: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Update contact priority
     * @param contactId The contact ID
     * @param priority The new priority
     * @param timestamp The timestamp of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE emergency_contacts 
        SET 
            priority = :priority, 
            modifiedAt = :timestamp
        WHERE id = :contactId
    """)
    suspend fun updateContactPriority(
        contactId: String,
        priority: Int,
        timestamp: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Get contacts for alert escalation
     * @param userId The user ID
     * @param severity The alert severity
     * @return List of emergency contact entities for escalation
     */
    @Query("""
        SELECT * FROM emergency_contacts 
        WHERE userId = :userId 
        AND (
            (alertSeverityThreshold = 'ALL') OR
            (alertSeverityThreshold = 'HIGH' AND :severity IN ('HIGH', 'CRITICAL')) OR
            (alertSeverityThreshold = 'CRITICAL' AND :severity = 'CRITICAL')
        )
        ORDER BY priority DESC, isPrimary DESC, name ASC
    """)
    suspend fun getContactsForAlertEscalation(
        userId: String,
        severity: String
    ): List<EmergencyContactEntity>

    /**
     * Alert History and Notification Tracking
     */

    /**
     * Get alert history for a specific user
     * @param userId The user ID
     * @param limit Maximum number of alerts to return
     * @return List of health alert entities
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getAlertHistory(userId: String, limit: Int = 100): List<HealthAlertEntity>

    /**
     * Get alert history for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @param limit Maximum number of alerts to return
     * @return Flow emitting list of health alert entities
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    fun getAlertHistoryAsFlow(userId: String, limit: Int = 100): Flow<List<HealthAlertEntity>>

    /**
     * Get alert history by metric type
     * @param userId The user ID
     * @param metricType The metric type to filter by
     * @param limit Maximum number of alerts to return
     * @return List of health alert entities for the specified metric type
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId AND metricType = :metricType 
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getAlertHistoryByMetricType(
        userId: String,
        metricType: String,
        limit: Int = 50
    ): List<HealthAlertEntity>

    /**
     * Get recent alerts
     * @param userId The user ID
     * @param hours Number of hours to look back
     * @return List of recent health alert entities
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        AND timestamp >= datetime('now', '-' || :hours || ' hours')
        ORDER BY timestamp DESC
    """)
    suspend fun getRecentAlerts(userId: String, hours: Int = 24): List<HealthAlertEntity>

    /**
     * Get alert count by status
     * @param userId The user ID
     * @return List of alert counts by status
     */
    @Query("""
        SELECT 
            status,
            COUNT(*) as alertCount
        FROM health_alerts
        WHERE userId = :userId
        GROUP BY status
        ORDER BY alertCount DESC
    """)
    suspend fun getAlertCountByStatus(userId: String): List<AlertCountByStatus>

    /**
     * Get alert count by severity
     * @param userId The user ID
     * @return List of alert counts by severity
     */
    @Query("""
        SELECT 
            severity,
            COUNT(*) as alertCount
        FROM health_alerts
        WHERE userId = :userId
        GROUP BY severity
        ORDER BY 
            CASE severity
                WHEN 'CRITICAL' THEN 1
                WHEN 'HIGH' THEN 2
                WHEN 'MEDIUM' THEN 3
                WHEN 'LOW' THEN 4
                ELSE 5
            END
    """)
    suspend fun getAlertCountBySeverity(userId: String): List<AlertCountBySeverity>

    /**
     * Get alert count by metric type
     * @param userId The user ID
     * @return List of alert counts by metric type
     */
    @Query("""
        SELECT 
            metricType,
            COUNT(*) as alertCount
        FROM health_alerts
        WHERE userId = :userId
        GROUP BY metricType
        ORDER BY alertCount DESC
    """)
    suspend fun getAlertCountByMetricType(userId: String): List<AlertCountByMetricType>

    /**
     * Get notification history
     * @param userId The user ID
     * @param limit Maximum number of alerts to return
     * @return List of health alert entities with notification info
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        AND isNotified = 1
        ORDER BY notificationTime DESC
        LIMIT :limit
    """)
    suspend fun getNotificationHistory(userId: String, limit: Int = 50): List<HealthAlertEntity>

    /**
     * Get unnotified alerts
     * @param userId The user ID
     * @return List of unnotified health alert entities
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        AND isNotified = 0
        AND status IN ('NEW', 'ACTIVE')
        ORDER BY timestamp DESC
    """)
    suspend fun getUnnotifiedAlerts(userId: String): List<HealthAlertEntity>

    /**
     * Mark alert as notified
     * @param alertId The alert ID
     * @param notificationTime The notification time
     * @param updateTime The time of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_alerts 
        SET 
            isNotified = 1, 
            notificationTime = :notificationTime,
            modifiedAt = :updateTime
        WHERE id = :alertId
    """)
    suspend fun markAsNotified(
        alertId: String,
        notificationTime: LocalDateTime = LocalDateTime.now(),
        updateTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Get daily notification count
     * @param userId The user ID
     * @param days Number of days to look back
     * @return List of daily notification counts
     */
    @Query("""
        SELECT 
            date(notificationTime) as notificationDate,
            COUNT(*) as notificationCount
        FROM health_alerts
        WHERE userId = :userId 
        AND isNotified = 1
        AND date(notificationTime) >= date('now', '-' || :days || ' days')
        GROUP BY date(notificationTime)
        ORDER BY notificationDate DESC
    """)
    suspend fun getDailyNotificationCount(
        userId: String,
        days: Int = 7
    ): List<DailyNotificationCount>

    /**
     * Smart Alert Filtering and Prioritization
     */

    /**
     * Get active alerts
     * @param userId The user ID
     * @return List of active health alert entities
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        AND status IN ('NEW', 'ACTIVE', 'ACKNOWLEDGED')
        ORDER BY 
            CASE severity
                WHEN 'CRITICAL' THEN 1
                WHEN 'HIGH' THEN 2
                WHEN 'MEDIUM' THEN 3
                WHEN 'LOW' THEN 4
                ELSE 5
            END,
            timestamp DESC
    """)
    suspend fun getActiveAlerts(userId: String): List<HealthAlertEntity>

    /**
     * Get active alerts as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of active health alert entities
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        AND status IN ('NEW', 'ACTIVE', 'ACKNOWLEDGED')
        ORDER BY 
            CASE severity
                WHEN 'CRITICAL' THEN 1
                WHEN 'HIGH' THEN 2
                WHEN 'MEDIUM' THEN 3
                WHEN 'LOW' THEN 4
                ELSE 5
            END,
            timestamp DESC
    """)
    fun getActiveAlertsAsFlow(userId: String): Flow<List<HealthAlertEntity>>

    /**
     * Get critical alerts
     * @param userId The user ID
     * @return List of critical health alert entities
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        AND severity = 'CRITICAL'
        AND status IN ('NEW', 'ACTIVE', 'ACKNOWLEDGED')
        ORDER BY timestamp DESC
    """)
    suspend fun getCriticalAlerts(userId: String): List<HealthAlertEntity>

    /**
     * Get critical alerts as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of critical health alert entities
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        AND severity = 'CRITICAL'
        AND status IN ('NEW', 'ACTIVE', 'ACKNOWLEDGED')
        ORDER BY timestamp DESC
    """)
    fun getCriticalAlertsAsFlow(userId: String): Flow<List<HealthAlertEntity>>

    /**
     * Get alerts requiring immediate attention
     * @param userId The user ID
     * @return List of health alert entities requiring immediate attention
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        AND (
            (severity = 'CRITICAL') OR
            (severity = 'HIGH' AND requiresImmediate = 1)
        )
        AND status IN ('NEW', 'ACTIVE')
        ORDER BY 
            CASE severity
                WHEN 'CRITICAL' THEN 1
                WHEN 'HIGH' THEN 2
                ELSE 3
            END,
            timestamp DESC
    """)
    suspend fun getAlertsRequiringImmediateAttention(userId: String): List<HealthAlertEntity>

    /**
     * Get alerts requiring immediate attention as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of health alert entities requiring immediate attention
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        AND (
            (severity = 'CRITICAL') OR
            (severity = 'HIGH' AND requiresImmediate = 1)
        )
        AND status IN ('NEW', 'ACTIVE')
        ORDER BY 
            CASE severity
                WHEN 'CRITICAL' THEN 1
                WHEN 'HIGH' THEN 2
                ELSE 3
            END,
            timestamp DESC
    """)
    fun getAlertsRequiringImmediateAttentionAsFlow(userId: String): Flow<List<HealthAlertEntity>>

    /**
     * Get similar alerts
     * @param alertId The alert ID
     * @param userId The user ID
     * @param hours Time window in hours
     * @return List of similar health alert entities
     */
    @Query("""
        SELECT * FROM health_alerts a
        WHERE a.userId = :userId
        AND a.id != :alertId
        AND a.metricType = (SELECT metricType FROM health_alerts WHERE id = :alertId)
        AND a.timestamp >= datetime((SELECT timestamp FROM health_alerts WHERE id = :alertId), '-' || :hours || ' hours')
        AND a.timestamp <= datetime((SELECT timestamp FROM health_alerts WHERE id = :alertId), '+' || :hours || ' hours')
        ORDER BY a.timestamp DESC
    """)
    suspend fun getSimilarAlerts(
        alertId: String,
        userId: String,
        hours: Int = 24
    ): List<HealthAlertEntity>

    /**
     * Get recurring alerts
     * @param userId The user ID
     * @param metricType The metric type
     * @param days Time window in days
     * @param minOccurrences Minimum number of occurrences
     * @return List of recurring alert patterns
     */
    @Query("""
        WITH alert_counts AS (
            SELECT 
                metricType,
                COUNT(*) as alertCount
            FROM health_alerts
            WHERE userId = :userId
            AND (:metricType IS NULL OR metricType = :metricType)
            AND timestamp >= datetime('now', '-' || :days || ' days')
            GROUP BY metricType
            HAVING COUNT(*) >= :minOccurrences
        )
        SELECT 
            a.metricType,
            ac.alertCount,
            MIN(a.timestamp) as firstOccurrence,
            MAX(a.timestamp) as lastOccurrence,
            (
                SELECT AVG(value)
                FROM health_alerts
                WHERE userId = :userId
                AND metricType = a.metricType
                AND timestamp >= datetime('now', '-' || :days || ' days')
            ) as avgValue,
            (
                SELECT GROUP_CONCAT(id)
                FROM health_alerts
                WHERE userId = :userId
                AND metricType = a.metricType
                AND timestamp >= datetime('now', '-' || :days || ' days')
                ORDER BY timestamp DESC
            ) as alertIds
        FROM health_alerts a
        JOIN alert_counts ac ON a.metricType = ac.metricType
        WHERE a.userId = :userId
        AND (:metricType IS NULL OR a.metricType = :metricType)
        AND a.timestamp >= datetime('now', '-' || :days || ' days')
        GROUP BY a.metricType
        ORDER BY alertCount DESC
    """)
    suspend fun getRecurringAlerts(
        userId: String,
        metricType: String? = null,
        days: Int = 30,
        minOccurrences: Int = 3
    ): List<RecurringAlertPattern>

    /**
     * Filter false alarms
     * @param userId The user ID
     * @param falseAlarmThreshold False alarm threshold (percentage)
     * @return List of potential false alarm patterns
     */
    @Query("""
        WITH rule_stats AS (
            SELECT 
                ruleId,
                COUNT(*) as totalTriggers,
                SUM(CASE WHEN status = 'FALSE_ALARM' THEN 1 ELSE 0 END) as falseAlarms,
                (SUM(CASE WHEN status = 'FALSE_ALARM' THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as falseAlarmRate
            FROM health_alerts
            WHERE userId = :userId
            AND ruleId IS NOT NULL
            GROUP BY ruleId
            HAVING COUNT(*) >= 5 AND falseAlarmRate >= :falseAlarmThreshold
        )
        SELECT 
            r.id as ruleId,
            r.name as ruleName,
            r.metricType,
            r.conditionType,
            r.thresholdValue,
            rs.totalTriggers,
            rs.falseAlarms,
            rs.falseAlarmRate
        FROM rule_stats rs
        JOIN alert_rules r ON rs.ruleId = r.id
        ORDER BY falseAlarmRate DESC
    """)
    suspend fun filterFalseAlarms(
        userId: String,
        falseAlarmThreshold: Double = 70.0
    ): List<FalseAlarmPattern>

    /**
     * Alert Escalation and Response Management
     */

    /**
     * Update alert status
     * @param alertId The alert ID
     * @param status The new status
     * @param notes Optional notes about the status change
     * @param updateTime The time of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_alerts 
        SET 
            status = :status, 
            notes = CASE WHEN :notes IS NULL THEN notes ELSE :notes END,
            modifiedAt = :updateTime
        WHERE id = :alertId
    """)
    suspend fun updateAlertStatus(
        alertId: String,
        status: String,
        notes: String? = null,
        updateTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Acknowledge alert
     * @param alertId The alert ID
     * @param notes Optional acknowledgment notes
     * @param updateTime The time of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_alerts 
        SET 
            status = 'ACKNOWLEDGED', 
            acknowledgedAt = :updateTime,
            notes = CASE WHEN :notes IS NULL THEN notes ELSE :notes END,
            modifiedAt = :updateTime
        WHERE id = :alertId
    """)
    suspend fun acknowledgeAlert(
        alertId: String,
        notes: String? = null,
        updateTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Resolve alert
     * @param alertId The alert ID
     * @param resolution The resolution description
     * @param updateTime The time of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_alerts 
        SET 
            status = 'RESOLVED', 
            resolvedAt = :updateTime,
            resolution = :resolution,
            modifiedAt = :updateTime
        WHERE id = :alertId
    """)
    suspend fun resolveAlert(
        alertId: String,
        resolution: String,
        updateTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Mark as false alarm
     * @param alertId The alert ID
     * @param reason The reason for false alarm
     * @param updateTime The time of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_alerts 
        SET 
            status = 'FALSE_ALARM', 
            resolvedAt = :updateTime,
            resolution = :reason,
            modifiedAt = :updateTime
        WHERE id = :alertId
    """)
    suspend fun markAsFalseAlarm(
        alertId: String,
        reason: String,
        updateTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Escalate alert
     * @param alertId The alert ID
     * @param escalationLevel The escalation level
     * @param contactIds Comma-separated list of contact IDs
     * @param updateTime The time of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_alerts 
        SET 
            status = 'ESCALATED', 
            escalatedAt = :updateTime,
            escalationLevel = :escalationLevel,
            escalatedToContactIds = :contactIds,
            modifiedAt = :updateTime
        WHERE id = :alertId
    """)
    suspend fun escalateAlert(
        alertId: String,
        escalationLevel: Int,
        contactIds: String,
        updateTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Get escalated alerts
     * @param userId The user ID
     * @return List of escalated health alert entities
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        AND status = 'ESCALATED'
        ORDER BY escalatedAt DESC
    """)
    suspend fun getEscalatedAlerts(userId: String): List<HealthAlertEntity>

    /**
     * Get alert response time statistics
     * @param userId The user ID
     * @return Alert response time statistics
     */
    @Query("""
        WITH response_times AS (
            SELECT 
                id,
                CASE 
                    WHEN acknowledgedAt IS NOT NULL THEN (julianday(acknowledgedAt) - julianday(timestamp)) * 24 * 60
                    ELSE NULL
                END as acknowledgeMinutes,
                CASE 
                    WHEN resolvedAt IS NOT NULL THEN (julianday(resolvedAt) - julianday(timestamp)) * 24 * 60
                    ELSE NULL
                END as resolveMinutes
            FROM health_alerts
            WHERE userId = :userId
            AND (acknowledgedAt IS NOT NULL OR resolvedAt IS NOT NULL)
        )
        SELECT 
            AVG(acknowledgeMinutes) as avgAcknowledgeMinutes,
            MIN(acknowledgeMinutes) as minAcknowledgeMinutes,
            MAX(acknowledgeMinutes) as maxAcknowledgeMinutes,
            AVG(resolveMinutes) as avgResolveMinutes,
            MIN(resolveMinutes) as minResolveMinutes,
            MAX(resolveMinutes) as maxResolveMinutes,
            (
                SELECT COUNT(*)
                FROM health_alerts
                WHERE userId = :userId
                AND acknowledgedAt IS NOT NULL
            ) as acknowledgedCount,
            (
                SELECT COUNT(*)
                FROM health_alerts
                WHERE userId = :userId
                AND resolvedAt IS NOT NULL
            ) as resolvedCount
        FROM response_times
    """)
    suspend fun getAlertResponseTimeStats(userId: String): AlertResponseTimeStats?

    /**
     * Get alert response time by severity
     * @param userId The user ID
     * @return Alert response time by severity
     */
    @Query("""
        SELECT 
            severity,
            AVG(CASE WHEN acknowledgedAt IS NOT NULL 
                THEN (julianday(acknowledgedAt) - julianday(timestamp)) * 24 * 60 
                ELSE NULL END) as avgAcknowledgeMinutes,
            AVG(CASE WHEN resolvedAt IS NOT NULL 
                THEN (julianday(resolvedAt) - julianday(timestamp)) * 24 * 60 
                ELSE NULL END) as avgResolveMinutes,
            COUNT(*) as alertCount
        FROM health_alerts
        WHERE userId = :userId
        AND (acknowledgedAt IS NOT NULL OR resolvedAt IS NOT NULL)
        GROUP BY severity
        ORDER BY 
            CASE severity
                WHEN 'CRITICAL' THEN 1
                WHEN 'HIGH' THEN 2
                WHEN 'MEDIUM' THEN 3
                WHEN 'LOW' THEN 4
                ELSE 5
            END
    """)
    suspend fun getAlertResponseTimeBySeverity(userId: String): List<AlertResponseTimeBySeverity>

    /**
     * Get escalation effectiveness
     * @param userId The user ID
     * @return Escalation effectiveness metrics
     */
    @Query("""
        WITH escalation_stats AS (
            SELECT 
                COUNT(*) as totalEscalations,
                SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END) as resolvedAfterEscalation,
                AVG(CASE WHEN resolvedAt IS NOT NULL AND escalatedAt IS NOT NULL 
                    THEN (julianday(resolvedAt) - julianday(escalatedAt)) * 24 * 60 
                    ELSE NULL END) as avgMinutesToResolveAfterEscalation
            FROM health_alerts
            WHERE userId = :userId
            AND escalatedAt IS NOT NULL
        )
        SELECT 
            totalEscalations,
            resolvedAfterEscalation,
            CASE 
                WHEN totalEscalations > 0 THEN (resolvedAfterEscalation * 100.0 / totalEscalations)
                ELSE 0
            END as resolutionRateAfterEscalation,
            avgMinutesToResolveAfterEscalation
        FROM escalation_stats
    """)
    suspend fun getEscalationEffectiveness(userId: String): EscalationEffectiveness?

    /**
     * Clinical Threshold Monitoring
     */

    /**
     * Get alerts exceeding clinical thresholds
     * @param userId The user ID
     * @param days Time window in days
     * @return List of health alert entities exceeding clinical thresholds
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        AND isClinicallySignificant = 1
        AND timestamp >= datetime('now', '-' || :days || ' days')
        ORDER BY timestamp DESC
    """)
    suspend fun getAlertsExceedingClinicalThresholds(
        userId: String,
        days: Int = 30
    ): List<HealthAlertEntity>

    /**
     * Get clinical threshold violations by metric type
     * @param userId The user ID
     * @param days Time window in days
     * @return Clinical threshold violations by metric type
     */
    @Query("""
        SELECT 
            metricType,
            COUNT(*) as violationCount,
            MIN(timestamp) as firstViolation,
            MAX(timestamp) as lastViolation,
            AVG(value) as avgValue
        FROM health_alerts
        WHERE userId = :userId 
        AND isClinicallySignificant = 1
        AND timestamp >= datetime('now', '-' || :days || ' days')
        GROUP BY metricType
        ORDER BY violationCount DESC
    """)
    suspend fun getClinicalThresholdViolationsByMetricType(
        userId: String,
        days: Int = 30
    ): List<ClinicalThresholdViolation>

    /**
     * Get clinical threshold trends
     * @param userId The user ID
     * @param metricType The metric type
     * @param days Time window in days
     * @return Clinical threshold trend data
     */
    @Query("""
        WITH daily_violations AS (
            SELECT 
                date(timestamp) as violationDate,
                COUNT(*) as dailyViolationCount
            FROM health_alerts
            WHERE userId = :userId 
            AND (:metricType IS NULL OR metricType = :metricType)
            AND isClinicallySignificant = 1
            AND timestamp >= datetime('now', '-' || :days || ' days')
            GROUP BY date(timestamp)
        )
        SELECT 
            violationDate,
            dailyViolationCount,
            SUM(dailyViolationCount) OVER (
                ORDER BY violationDate 
                ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
            ) as cumulativeViolationCount
        FROM daily_violations
        ORDER BY violationDate
    """)
    suspend fun getClinicalThresholdTrends(
        userId: String,
        metricType: String? = null,
        days: Int = 30
    ): List<ClinicalThresholdTrend>

    /**
     * Get metric values requiring medical review
     * @param userId The user ID
     * @param days Time window in days
     * @return List of metric values requiring medical review
     */
    @Query("""
        SELECT 
            metricType,
            MIN(value) as minValue,
            MAX(value) as maxValue,
            AVG(value) as avgValue,
            COUNT(*) as occurrenceCount,
            MIN(timestamp) as firstOccurrence,
            MAX(timestamp) as lastOccurrence,
            (
                SELECT GROUP_CONCAT(id)
                FROM health_alerts
                WHERE userId = :userId
                AND metricType = a.metricType
                AND requiresMedicalReview = 1
                AND timestamp >= datetime('now', '-' || :days || ' days')
                ORDER BY timestamp DESC
            ) as alertIds
        FROM health_alerts a
        WHERE userId = :userId 
        AND requiresMedicalReview = 1
        AND timestamp >= datetime('now', '-' || :days || ' days')
        GROUP BY metricType
        ORDER BY occurrenceCount DESC
    """)
    suspend fun getMetricValuesRequiringMedicalReview(
        userId: String,
        days: Int = 30
    ): List<MedicalReviewMetric>

    /**
     * Update medical review status
     * @param alertId The alert ID
     * @param isReviewed Whether the alert has been medically reviewed
     * @param reviewNotes Medical review notes
     * @param reviewedBy Who reviewed the alert
     * @param updateTime The time of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_alerts 
        SET 
            isMedicallyReviewed = :isReviewed, 
            medicalReviewNotes = :reviewNotes,
            medicallyReviewedBy = :reviewedBy,
            medicallyReviewedAt = :updateTime,
            modifiedAt = :updateTime
        WHERE id = :alertId
    """)
    suspend fun updateMedicalReviewStatus(
        alertId: String,
        isReviewed: Boolean,
        reviewNotes: String?,
        reviewedBy: String?,
        updateTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Get medically reviewed alerts
     * @param userId The user ID
     * @param days Time window in days
     * @return List of medically reviewed health alert entities
     */
    @Query("""
        SELECT * FROM health_alerts 
        WHERE userId = :userId 
        AND isMedicallyReviewed = 1
        AND timestamp >= datetime('now', '-' || :days || ' days')
        ORDER BY medicallyReviewedAt DESC
    """)
    suspend fun getMedicallyReviewedAlerts(
        userId: String,
        days: Int = 30
    ): List<HealthAlertEntity>

    /**
     * Data classes for query results
     */

    /**
     * Rule effectiveness metrics
     */
    data class RuleEffectiveness(
        val totalTriggers: Int,
        val resolvedAlerts: Int,
        val falseAlarms: Int,
        val acknowledgedAlerts: Int,
        val escalatedAlerts: Int,
        val falseAlarmRate: Double,
        val actionableAlertRate: Double
    )

    /**
     * Alert count by status
     */
    data class AlertCountByStatus(
        val status: String,
        val alertCount: Int
    )

    /**
     * Alert count by severity
     */
    data class AlertCountBySeverity(
        val severity: String,
        val alertCount: Int
    )

    /**
     * Alert count by metric type
     */
    data class AlertCountByMetricType(
        val metricType: String,
        val alertCount: Int
    )

    /**
     * Daily notification count
     */
    data class DailyNotificationCount(
        val notificationDate: String,
        val notificationCount: Int
    )

    /**
     * Recurring alert pattern
     */
    data class RecurringAlertPattern(
        val metricType: String,
        val alertCount: Int,
        val firstOccurrence: LocalDateTime,
        val lastOccurrence: LocalDateTime,
        val avgValue: Double,
        val alertIds: String
    )

    /**
     * False alarm pattern
     */
    data class FalseAlarmPattern(
        val ruleId: String,
        val ruleName: String,
        val metricType: String,
        val conditionType: String,
        val thresholdValue: Double,
        val totalTriggers: Int,
        val falseAlarms: Int,
        val falseAlarmRate: Double
    )

    /**
     * Alert response time statistics
     */
    data class AlertResponseTimeStats(
        val avgAcknowledgeMinutes: Double?,
        val minAcknowledgeMinutes: Double?,
        val maxAcknowledgeMinutes: Double?,
        val avgResolveMinutes: Double?,
        val minResolveMinutes: Double?,
        val maxResolveMinutes: Double?,
        val acknowledgedCount: Int,
        val resolvedCount: Int
    )

    /**
     * Alert response time by severity
     */
    data class AlertResponseTimeBySeverity(
        val severity: String,
        val avgAcknowledgeMinutes: Double?,
        val avgResolveMinutes: Double?,
        val alertCount: Int
    )

    /**
     * Escalation effectiveness metrics
     */
    data class EscalationEffectiveness(
        val totalEscalations: Int,
        val resolvedAfterEscalation: Int,
        val resolutionRateAfterEscalation: Double,
        val avgMinutesToResolveAfterEscalation: Double?
    )

    /**
     * Clinical threshold violation
     */
    data class ClinicalThresholdViolation(
        val metricType: String,
        val violationCount: Int,
        val firstViolation: LocalDateTime,
        val lastViolation: LocalDateTime,
        val avgValue: Double
    )

    /**
     * Clinical threshold trend
     */
    data class ClinicalThresholdTrend(
        val violationDate: String,
        val dailyViolationCount: Int,
        val cumulativeViolationCount: Int
    )

    /**
     * Medical review metric
     */
    data class MedicalReviewMetric(
        val metricType: String,
        val minValue: Double,
        val maxValue: Double,
        val avgValue: Double,
        val occurrenceCount: Int,
        val firstOccurrence: LocalDateTime,
        val lastOccurrence: LocalDateTime,
        val alertIds: String
    )
}

package com.globaltrade.ejb.timer;

import com.globaltrade.common.entity.CustomsDeclaration;
import com.globaltrade.ejb.service.CustomsService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.ejb.ScheduleExpression;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Programmatic Timer for customs submission deadline tracking.
 *
 * <p><strong>Timer Type: PROGRAMMATIC (CalendarTimer)</strong> — Uses
 * {@code TimerService.createCalendarTimer()} because customs deadlines are
 * dynamic — each declaration has a unique submission deadline. A single
 * declarative @Schedule cannot handle variable deadlines per entity.</p>
 *
 * <p><strong>Critical Analysis — Dynamic Timer Creation:</strong>
 * Unlike the fixed-interval ShipmentTrackingTimer, this timer creates
 * calendar-based timers that fire at specific dates. Each customs declaration's
 * deadline triggers a separate timer, demonstrating the flexibility of
 * programmatic timer creation for event-driven workflows.</p>
 *
 * <p><strong>Persistence: {@code persistent = true}</strong> — Deadline alerts
 * must survive server restarts. Missing a customs deadline can result in
 * penalties, cargo seizure, or trade sanctions.</p>
 */
@Singleton
@Startup
public class CustomsDeadlineTimer {

    private static final Logger LOGGER = Logger.getLogger(CustomsDeadlineTimer.class.getName());
    private static final String TIMER_PREFIX = "CustomsDeadline-";
    private static final int WARNING_DAYS_BEFORE = 3;

    @Resource
    private TimerService timerService;

    @EJB
    private CustomsService customsService;

    /**
     * On startup, scan for declarations with approaching deadlines
     * and create timers for each.
     */
    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, "Initialising customs deadline timer system...");
        scheduleUpcomingDeadlines();
    }

    /**
     * Scans for pending declarations and creates calendar timers
     * for those with deadlines within the warning period.
     */
    public void scheduleUpcomingDeadlines() {
        try {
            List<CustomsDeclaration> upcoming =
                    customsService.findApproachingDeadlines(WARNING_DAYS_BEFORE);

            // Cancel existing deadline timers to avoid duplicates
            for (Timer timer : timerService.getTimers()) {
                if (timer.getInfo() != null &&
                        timer.getInfo().toString().startsWith(TIMER_PREFIX)) {
                    timer.cancel();
                }
            }

            for (CustomsDeclaration declaration : upcoming) {
                createDeadlineTimer(declaration);
            }

            LOGGER.log(Level.INFO, "Scheduled {0} customs deadline alerts", upcoming.size());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error scheduling customs deadlines", e);
        }
    }

    /**
     * Creates a calendar timer that fires on the warning date
     * (3 days before the actual deadline).
     */
    public void createDeadlineTimer(CustomsDeclaration declaration) {
        LocalDate warningDate = declaration.getSubmissionDeadline()
                .minusDays(WARNING_DAYS_BEFORE);

        // Only create timer if warning date is in the future
        if (warningDate.isAfter(LocalDate.now())) {
            ScheduleExpression schedule = new ScheduleExpression()
                    .year(warningDate.getYear())
                    .month(warningDate.getMonthValue())
                    .dayOfMonth(warningDate.getDayOfMonth())
                    .hour(9)    // Alert at 9:00 AM
                    .minute(0);

            TimerConfig config = new TimerConfig();
            config.setInfo(TIMER_PREFIX + declaration.getDeclarationNumber());
            config.setPersistent(true); // Must survive restart — critical deadline

            timerService.createCalendarTimer(schedule, config);

            LOGGER.log(Level.INFO,
                    "Deadline timer created: Declaration={0}, Deadline={1}, Alert={2}",
                    new Object[]{declaration.getDeclarationNumber(),
                            declaration.getSubmissionDeadline(), warningDate});
        } else {
            // Warning date has already passed — immediate alert
            LOGGER.log(Level.SEVERE,
                    "URGENT: Customs deadline imminent! Declaration={0}, Deadline={1}",
                    new Object[]{declaration.getDeclarationNumber(),
                            declaration.getSubmissionDeadline()});
        }
    }

    /**
     * Timeout callback — fired when a customs deadline warning is due.
     */
    @Timeout
    public void handleDeadlineAlert(Timer timer) {
        String timerInfo = timer.getInfo().toString();
        String declarationNumber = timerInfo.replace(TIMER_PREFIX, "");

        LOGGER.log(Level.WARNING,
                "=== CUSTOMS DEADLINE ALERT: Declaration {0} ===", declarationNumber);
        LOGGER.log(Level.WARNING,
                "Submission deadline is in {0} days. Immediate action required!",
                WARNING_DAYS_BEFORE);

        // Log detailed alert for compliance tracking
        LOGGER.log(Level.WARNING,
                "Action Required: Submit customs declaration {0} before deadline " +
                "to avoid penalties and cargo seizure.", declarationNumber);
    }
}

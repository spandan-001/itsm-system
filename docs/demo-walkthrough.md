# ITSM demo video

Open http://localhost:3000 on the computer running the frontend and the Supabase-backed Spring server. Use separate browser profiles for each role so you can switch between dashboards during the recording. Sign in before you start recording.

| Role | Demo sign-in | What to show |
| --- | --- | --- |
| Employee, Maya | employee@itsm-video.example.test | Report an incident, answer the technician, verify the fix |
| Technician, Ravi | technician@itsm-video.example.test | Acknowledge the incident, ask a question, resolve it |
| Administrator, Asha | admin@itsm-video.example.test | Review routing, SLA and workload, assign Ravi if needed |
| Second employee, Noor | employee2@itsm-video.example.test | Optional privacy check using Maya's incident link |

Passwords are available to the database owner in Supabase SQL Editor. Run this off camera. These addresses are synthetic and have no mailbox.

```sql
select name, email, password
from itsm_demo_private.readable_user_passwords
where email like '%@itsm-video.example.test'
order by email;
```

## Recording sequence

1. **Maya reports the issue.** Choose **Report an incident**. For a new take, use a distinct issue such as “Studio label printer cuts every shipping label in half.” Describe the symptoms, show the category suggestion, choose Hardware, select impact and urgency, choose Printing Service, then click **Submit incident**. Note the new incident number. Reusing an open report with similar wording may trigger duplicate detection.
2. **Asha checks routing.** Open the admin dashboard and find that number under **Incident management**. Show its priority, technician and SLA due time. If another technician was selected or the incident is queued, choose **Ravi Demo** from the row's technician dropdown. If Ravi is already assigned, show the automatic assignment. Point out the **Assignment strategy** control above the filters: it shows the active strategy (Smart by default), and choosing Skill, Load or Smart and clicking **Change strategy** switches how new incidents are routed, without a restart.
3. **Ravi investigates.** Open the assigned incident and click **Acknowledge**, then **Start work**. Under **Note type**, choose **Ask the reporter**, enter a question and click **Add note**. Show the response and resolution targets under **SLA**.
4. **Maya replies.** Open the same incident from **Notifications** or **My incidents**. Refresh if needed. Choose **Answer the technician**, enter the answer and click **Add note**.
5. **Ravi resolves it.** Refresh the incident, read Maya's reply, enter a resolution summary and click **Resolve**.
6. **Maya confirms.** Refresh, click **Verify fix**, then **Close incident**. Choose a feedback rating and click **Send feedback**. Scroll to **Audit timeline** to show who did what and when.
7. **Asha wraps up.** Refresh the admin dashboard and show the closed incident alongside analytics. For an optional privacy check, paste the incident's local link into Noor's signed-in profile. The app should deny access.

For another take, create a fresh incident. The previous one stays available as a completed example. If the backend is restarted, use the Supabase profile and load the ignored local `.env.supabase` settings including `ITSM_ADMIN_USER_IDS`. On another host, the administrator's Supabase user UUID must be included in that server's allowlist.

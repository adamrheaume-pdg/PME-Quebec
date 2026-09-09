from pathlib import Path

p = Path('app/src/main/java/com/pmequebec/sondages2026/MainActivity.java')
s = p.read_text(encoding='utf-8')

if 'COUNTDOWN_ELECTION_2026' in s:
    print('Countdown already patched')
    raise SystemExit(0)

s = s.replace('import android.os.Bundle;\n', 'import android.os.Bundle;\nimport android.os.CountDownTimer;\n')
s = s.replace('import java.util.ArrayList;\n', 'import java.util.ArrayList;\nimport java.util.Calendar;\n')

s = s.replace(
    '    private TrendView trendView;\n',
    '    private TrendView trendView;\n    private CountDownTimer electionCountdown; // COUNTDOWN_ELECTION_2026\n'
)

needle = '''        content.addView(subtitle, matchWrap());\n\n        status = text("● Données locales prêtes — dernière vague intégrée : 6 septembre 2026", 13, Color.rgb(24,120,74), true);'''
replacement = '''        content.addView(subtitle, matchWrap());\n\n        LinearLayout countdownCard = new LinearLayout(this);\n        countdownCard.setOrientation(LinearLayout.VERTICAL);\n        countdownCard.setGravity(Gravity.CENTER);\n        countdownCard.setPadding(dp(12), dp(14), dp(12), dp(14));\n        countdownCard.setBackgroundColor(NAVY);\n        LinearLayout.LayoutParams countdownLp = matchWrap();\n        countdownLp.setMargins(0, dp(10), 0, dp(4));\n\n        TextView countdownLabel = text("ÉLECTION QUÉBÉCOISE • 5 OCTOBRE 2026", 13, Color.rgb(190,210,255), true);\n        countdownLabel.setGravity(Gravity.CENTER);\n        TextView countdown = text("Calcul du compte à rebours…", 24, Color.WHITE, true);\n        countdown.setGravity(Gravity.CENTER);\n        countdown.setPadding(0, dp(5), 0, 0);\n        countdownCard.addView(countdownLabel, matchWrap());\n        countdownCard.addView(countdown, matchWrap());\n        content.addView(countdownCard, countdownLp);\n        startElectionCountdown(countdown);\n\n        status = text("● Données locales prêtes — dernière vague intégrée : 6 septembre 2026", 13, Color.rgb(24,120,74), true);'''
if needle not in s:
    raise SystemExit('Could not locate subtitle/status insertion point')
s = s.replace(needle, replacement)

method_needle = '    private void loadPolls() {\n'
method = '''    private void startElectionCountdown(TextView countdown) {\n        Calendar target = Calendar.getInstance();\n        target.set(2026, Calendar.OCTOBER, 5, 0, 0, 0);\n        target.set(Calendar.MILLISECOND, 0);\n\n        long remaining = target.getTimeInMillis() - System.currentTimeMillis();\n        if (remaining <= 0) {\n            countdown.setText("JOUR DU SCRUTIN");\n            return;\n        }\n\n        electionCountdown = new CountDownTimer(remaining, 1000) {\n            @Override public void onTick(long ms) {\n                long totalSeconds = ms / 1000;\n                long days = totalSeconds / 86400;\n                long hours = (totalSeconds % 86400) / 3600;\n                long minutes = (totalSeconds % 3600) / 60;\n                long seconds = totalSeconds % 60;\n                countdown.setText(String.format(Locale.CANADA_FRENCH,\n                        "%d J  %02d H  %02d MIN  %02d S", days, hours, minutes, seconds));\n            }\n\n            @Override public void onFinish() {\n                countdown.setText("JOUR DU SCRUTIN");\n            }\n        }.start();\n    }\n\n    @Override\n    protected void onDestroy() {\n        if (electionCountdown != null) electionCountdown.cancel();\n        super.onDestroy();\n    }\n\n'''
if method_needle not in s:
    raise SystemExit('Could not locate loadPolls insertion point')
s = s.replace(method_needle, method + method_needle)

p.write_text(s, encoding='utf-8')
print('Election countdown patched successfully')

package cz.leo.us500screenbot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_ZONE = 101;
    private static final int PICK_MES = 102;
    private static final int PICK_US500 = 103;

    private Uri zoneUri;
    private Uri mesUri;
    private Uri us500Uri;

    private ImageView zonePreview;
    private ImageView mesPreview;
    private ImageView us500Preview;
    private TextView zoneState;
    private TextView mesState;
    private TextView us500State;
    private ProgressBar progress;
    private LinearLayout reviewPanel;
    private LinearLayout resultPanel;
    private Spinner platformSpinner;
    private Spinner candleTimeSpinner;

    private EditText zoneHighInput;
    private EditText zoneLowInput;
    private EditText mesCloseInput;
    private EditText usCloseInput;

    private TextView resultPlatform;
    private TextView resultOffset;
    private TextView resultHigh;
    private TextView resultLow;
    private TextView resultMid;
    private TextView resultTime;

    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = vertical(16);
        root.setPadding(dp(16), dp(18), dp(16), dp(36));
        root.setBackgroundColor(Color.rgb(244, 247, 250));
        scroll.addView(root);

        TextView title = label("US500 SCREEN BOT", 24, true);
        title.setTextColor(Color.rgb(16, 42, 67));
        root.addView(title);
        TextView subtitle = label("3 screenshoty → automatický přepočet", 15, false);
        subtitle.setPadding(0, dp(3), 0, dp(14));
        root.addView(subtitle);

        root.addView(label("Platforma US500", 15, true));
        platformSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Rebels Funding", "Fintokei"});
        platformSpinner.setAdapter(adapter);
        root.addView(platformSpinner, matchWrap());

        root.addView(label("Čas uzavřené 5m svíčky", 15, true));
        candleTimeSpinner = new Spinner(this);
        List<String> times = buildFiveMinuteTimes();
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, times);
        candleTimeSpinner.setAdapter(timeAdapter);
        candleTimeSpinner.setSelection(defaultDelayedTimeIndex());
        root.addView(candleTimeSpinner, matchWrap());
        TextView timeHelp = label("Čas vyber jednou. OCR ho už nečte ani neporovnává.", 12, false);
        timeHelp.setPadding(0, 0, 0, dp(6));
        root.addView(timeHelp);

        zonePreview = preview();
        zoneState = label("Nevybráno", 13, false);
        root.addView(screenshotCard("1. ENTRY ZÓNA MES", "Screenshot zprávy z Discordu", PICK_ZONE, zonePreview, zoneState));

        mesPreview = preview();
        mesState = label("Nevybráno", 13, false);
        root.addView(screenshotCard("2. MES SVÍČKA", "Musí být viditelná hodnota C / Close", PICK_MES, mesPreview, mesState));

        us500Preview = preview();
        us500State = label("Nevybráno", 13, false);
        root.addView(screenshotCard("3. US500 SVÍČKA", "Stejná svíčka jako zvolený čas; OCR čte jen Close", PICK_US500, us500Preview, us500State));

        Button analyze = primaryButton("ROZPOZNAT A SPOČÍTAT");
        analyze.setOnClickListener(v -> startRecognition());
        root.addView(analyze, matchHeight(56));

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(dp(44), dp(44));
        progressLp.gravity = Gravity.CENTER_HORIZONTAL;
        progressLp.setMargins(0, dp(10), 0, dp(10));
        root.addView(progress, progressLp);

        reviewPanel = buildReviewPanel();
        reviewPanel.setVisibility(View.GONE);
        root.addView(reviewPanel);

        resultPanel = buildResultPanel();
        resultPanel.setVisibility(View.GONE);
        root.addView(resultPanel);

        TextView privacy = label("Bez serveru • Bez placeného API • Screenshoty zůstávají v zařízení", 12, false);
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(0, dp(18), 0, 0);
        root.addView(privacy);
        return scroll;
    }

    private LinearLayout screenshotCard(String heading, String help, int requestCode, ImageView preview, TextView state) {
        LinearLayout card = vertical(10);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams cardLp = matchWrap();
        cardLp.setMargins(0, dp(12), 0, 0);
        card.setLayoutParams(cardLp);

        card.addView(label(heading, 16, true));
        card.addView(label(help, 13, false));
        Button pick = secondaryButton("VYBRAT SCREENSHOT");
        pick.setOnClickListener(v -> openImagePicker(requestCode));
        card.addView(pick, matchHeight(48));
        card.addView(preview, matchHeight(170));
        card.addView(state);
        return card;
    }

    private LinearLayout buildReviewPanel() {
        LinearLayout box = vertical(10);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, dp(14), 0, 0);
        box.setLayoutParams(lp);
        box.addView(label("KONTROLA ROZPOZNANÝCH HODNOT", 17, true));
        box.addView(label("Normálně nic neměň. Oprav pouze hodnotu, kterou OCR přečetlo špatně.", 13, false));

        zoneHighInput = numericField("MES zóna horní");
        zoneLowInput = numericField("MES zóna dolní");
        mesCloseInput = numericField("MES Close");
        usCloseInput = numericField("US500 Close");

        addField(box, "MES horní", zoneHighInput);
        addField(box, "MES dolní", zoneLowInput);
        addField(box, "MES Close", mesCloseInput);
        addField(box, "US500 Close", usCloseInput);

        Button recalc = primaryButton("PŘEPOČÍTAT Z KONTROLOVANÝCH HODNOT");
        recalc.setOnClickListener(v -> calculateFromFields(true));
        box.addView(recalc, matchHeight(54));
        return box;
    }

    private LinearLayout buildResultPanel() {
        LinearLayout box = vertical(9);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.setBackgroundColor(Color.rgb(232, 245, 233));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, dp(14), 0, 0);
        box.setLayoutParams(lp);
        box.addView(label("VÝSLEDEK – US500 ENTRY ZÓNA", 18, true));
        resultPlatform = label("", 15, true);
        resultTime = label("", 14, false);
        resultOffset = label("", 16, false);
        resultHigh = label("", 23, true);
        resultLow = label("", 23, true);
        resultMid = label("", 17, false);
        box.addView(resultPlatform);
        box.addView(resultTime);
        box.addView(resultOffset);
        box.addView(resultHigh);
        box.addView(resultLow);
        box.addView(resultMid);

        LinearLayout copyRow = horizontal(8);
        Button copyHigh = secondaryButton("KOPÍROVAT HORNÍ");
        Button copyLow = secondaryButton("KOPÍROVAT DOLNÍ");
        copyHigh.setOnClickListener(v -> copyValue(resultHigh.getTag()));
        copyLow.setOnClickListener(v -> copyValue(resultLow.getTag()));
        copyRow.addView(copyHigh, weighted());
        copyRow.addView(copyLow, weighted());
        box.addView(copyRow, matchWrap());

        Button copyBoth = primaryButton("KOPÍROVAT OBOJE");
        copyBoth.setOnClickListener(v -> {
            Object high = resultHigh.getTag();
            Object low = resultLow.getTag();
            if (high != null && low != null) copyValue(high + " / " + low);
        });
        box.addView(copyBoth, matchHeight(52));

        Button reset = secondaryButton("NOVÝ VÝPOČET");
        reset.setOnClickListener(v -> resetAll());
        box.addView(reset, matchHeight(48));
        return box;
    }

    private void openImagePicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {}

        if (requestCode == PICK_ZONE) {
            zoneUri = uri;
            zonePreview.setImageURI(uri);
            zoneState.setText("Připraveno");
        } else if (requestCode == PICK_MES) {
            mesUri = uri;
            mesPreview.setImageURI(uri);
            mesState.setText("Připraveno");
        } else if (requestCode == PICK_US500) {
            us500Uri = uri;
            us500Preview.setImageURI(uri);
            us500State.setText("Připraveno");
        }
    }

    private void startRecognition() {
        if (zoneUri == null || mesUri == null || us500Uri == null) {
            toast("Vyber všechny tři screenshoty.");
            return;
        }
        progress.setVisibility(View.VISIBLE);
        resultPanel.setVisibility(View.GONE);
        reviewPanel.setVisibility(View.GONE);
        setStates("Čtu OCR…");

        recognize(zoneUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    String zoneText = task.getResult();
                    return recognize(mesUri).continueWithTask(mesTask -> {
                        if (!mesTask.isSuccessful()) throw mesTask.getException();
                        String mesText = mesTask.getResult();
                        return recognize(us500Uri).continueWith(usTask -> {
                            if (!usTask.isSuccessful()) throw usTask.getException();
                            return new String[]{zoneText, mesText, usTask.getResult()};
                        });
                    });
                })
                .addOnSuccessListener(texts -> {
                    progress.setVisibility(View.GONE);
                    fillRecognizedValues(texts[0], texts[1], texts[2]);
                })
                .addOnFailureListener(error -> {
                    progress.setVisibility(View.GONE);
                    setStates("Chyba OCR");
                    showError("OCR se nepodařilo dokončit", error.getMessage());
                });
    }

    private Task<String> recognize(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            return recognizer.process(image).continueWith(task -> {
                if (!task.isSuccessful()) throw task.getException();
                Text text = task.getResult();
                return text == null ? "" : text.getText();
            });
        } catch (IOException ex) {
            return Tasks.forException(ex);
        }
    }

    private void fillRecognizedValues(String zoneText, String mesText, String usText) {
        TradeParser.Zone zone = TradeParser.parseZone(zoneText);
        BigDecimal mesClose = TradeParser.parseCandleClose(mesText);
        BigDecimal usClose = TradeParser.parseCandleClose(usText);

        zoneHighInput.setText(zone == null ? "" : plain(zone.high));
        zoneLowInput.setText(zone == null ? "" : plain(zone.low));
        mesCloseInput.setText(mesClose == null ? "" : plain(mesClose));
        usCloseInput.setText(usClose == null ? "" : plain(usClose));

        zoneState.setText(zone == null ? "Nelze bezpečně přečíst" : "Zóna rozpoznána");
        mesState.setText(mesClose == null ? "Chybí C / Close" : "MES Close: " + plain(mesClose));
        us500State.setText(usClose == null ? "Chybí C / Close" : "US500 Close: " + plain(usClose));
        reviewPanel.setVisibility(View.VISIBLE);

        if (zone != null && mesClose != null && usClose != null) {
            calculateFromFields(false);
        } else {
            toast("Zkontroluj rozpoznané hodnoty. Čas se už přes OCR nekontroluje.");
        }
    }

    private void calculateFromFields(boolean showErrors) {
        BigDecimal zoneHigh = TradeParser.parseDecimal(zoneHighInput.getText().toString());
        BigDecimal zoneLow = TradeParser.parseDecimal(zoneLowInput.getText().toString());
        BigDecimal mesClose = TradeParser.parseDecimal(mesCloseInput.getText().toString());
        BigDecimal usClose = TradeParser.parseDecimal(usCloseInput.getText().toString());
        String selectedTime = candleTimeSpinner.getSelectedItem().toString();

        if (zoneHigh == null || zoneLow == null || mesClose == null || usClose == null) {
            if (showErrors) showError("Chybí údaj", "Zkontroluj čtyři rozpoznané hodnoty.");
            return;
        }
        try {
            TradeCalculator.Result result = TradeCalculator.calculate(zoneHigh, zoneLow, mesClose, usClose);
            showResult(result, selectedTime);
        } catch (IllegalArgumentException ex) {
            if (showErrors) showError("Výpočet nelze provést", ex.getMessage());
        }
    }

    private void showResult(TradeCalculator.Result result, String time) {
        String platform = platformSpinner.getSelectedItem().toString();
        String high = plain(result.high);
        String low = plain(result.low);
        resultPlatform.setText("Platforma: " + platform);
        resultTime.setText("Čas uzavřené svíčky: " + time);
        resultOffset.setText("Offset: " + signed(result.offset));
        resultHigh.setText("HORNÍ: " + high);
        resultLow.setText("DOLNÍ: " + low);
        resultMid.setText("Midpoint: " + plain(result.midpoint));
        resultHigh.setTag(high);
        resultLow.setTag(low);
        resultPanel.setVisibility(View.VISIBLE);
        resultPanel.requestFocus();
    }

    private void copyValue(Object value) {
        if (value == null) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("US500 hodnota", value.toString()));
        toast("Zkopírováno: " + value);
    }

    private void resetAll() {
        zoneUri = mesUri = us500Uri = null;
        zonePreview.setImageDrawable(null);
        mesPreview.setImageDrawable(null);
        us500Preview.setImageDrawable(null);
        zoneState.setText("Nevybráno");
        mesState.setText("Nevybráno");
        us500State.setText("Nevybráno");
        reviewPanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
    }

    private void setStates(String text) {
        zoneState.setText(text);
        mesState.setText(text);
        us500State.setText(text);
    }

    private void addField(LinearLayout parent, String name, EditText input) {
        parent.addView(label(name, 13, true));
        parent.addView(input, matchHeight(50));
    }

    private EditText numericField(String hint) {
        EditText e = textField(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        return e;
    }

    private EditText textField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(17);
        e.setSingleLine(true);
        e.setPadding(dp(12), 0, dp(12), 0);
        e.setBackgroundColor(Color.rgb(245, 247, 250));
        return e;
    }

    private ImageView preview() {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(Color.rgb(232, 236, 240));
        return image;
    }

    private Button primaryButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackgroundColor(Color.rgb(14, 116, 144));
        return b;
    }

    private Button secondaryButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.rgb(16, 42, 67));
        b.setTextSize(13);
        b.setAllCaps(false);
        return b;
    }

    private TextView label(String text, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(Color.rgb(51, 65, 85));
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout vertical(int gapDp) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.FILL_HORIZONTAL);
        return l;
    }

    private LinearLayout horizontal(int gapDp) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        return l;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchHeight(int dp) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(dp));
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        return lp;
    }

    private List<String> buildFiveMinuteTimes() {
        List<String> times = new ArrayList<>(288);
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 5) {
                times.add(String.format(Locale.US, "%02d:%02d", hour, minute));
            }
        }
        return times;
    }

    private int defaultDelayedTimeIndex() {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, -10);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = (now.get(Calendar.MINUTE) / 5) * 5;
        return hour * 12 + minute / 5;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String plain(BigDecimal value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String signed(BigDecimal value) {
        return (value.signum() > 0 ? "+" : "") + plain(value);
    }

    private void showError(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message == null ? "Neznámá chyba." : message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        recognizer.close();
        super.onDestroy();
    }
}

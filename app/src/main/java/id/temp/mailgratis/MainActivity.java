package id.temp.mailgratis;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(13, 17, 28), CARD = Color.rgb(25, 32, 48), ACCENT = Color.rgb(111, 91, 255);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView emailText, timerText, statusText, inboxTitle;
    private LinearLayout inboxList;
    private String email = "", login = "", domain = "";
    private long expiresAt = 0;
    private boolean loading = false;
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            updateTimer();
            if (!email.isEmpty()) refreshInbox(false);
            handler.postDelayed(this, 15000);
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
        handler.post(poll);
        generateEmail();
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l;
    }
    private GradientDrawableCompat shape(int color, int radius) { return new GradientDrawableCompat(color, dp(radius)); }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout page = column(); page.setPadding(dp(20), dp(24), dp(20), dp(24)); scroll.addView(page);
        TextView brand = text("✉  TempMail Gratis", 25, Color.WHITE, true); page.addView(brand);
        TextView subtitle = text("Email sementara, cepat dan gratis", 14, Color.rgb(166,177,198), false);
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(-1,-2); subP.topMargin=dp(6); page.addView(subtitle,subP);

        LinearLayout emailCard = column(); emailCard.setPadding(dp(16),dp(16),dp(16),dp(16)); emailCard.setBackground(shape(CARD,18).drawable());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,-2); cp.topMargin=dp(24); page.addView(emailCard,cp);
        emailCard.addView(text("ALAMAT EMAIL ANDA",12,Color.rgb(166,177,198),true));
        emailText = text("Membuat email…",17,Color.WHITE,true); emailText.setTextIsSelectable(true);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1,-2); ep.topMargin=dp(12); emailCard.addView(emailText,ep);
        timerText = text("Sesi: --:--",13,Color.rgb(255,199,107),false);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1,-2); tp.topMargin=dp(10); emailCard.addView(timerText,tp);
        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        Button copy = button("Salin alamat"); copy.setOnClickListener(v -> copyEmail());
        Button newEmail = button("Email baru"); newEmail.setOnClickListener(v -> generateEmail());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0,dp(48),1); bp.topMargin=dp(14); bp.rightMargin=dp(6);
        actions.addView(copy,bp); LinearLayout.LayoutParams bp2 = new LinearLayout.LayoutParams(0,dp(48),1); bp2.topMargin=dp(14); bp2.leftMargin=dp(6); actions.addView(newEmail,bp2);
        emailCard.addView(actions);

        LinearLayout inboxCard = column(); inboxCard.setPadding(dp(16),dp(16),dp(16),dp(16)); inboxCard.setBackground(shape(CARD,18).drawable());
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1,-2); ip.topMargin=dp(16); page.addView(inboxCard,ip);
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        inboxTitle = text("Kotak masuk",19,Color.WHITE,true); header.addView(inboxTitle,new LinearLayout.LayoutParams(0,-2,1));
        Button refresh = button("↻ Refresh"); refresh.setOnClickListener(v -> refreshInbox(true)); header.addView(refresh);
        inboxCard.addView(header);
        statusText = text("Menyiapkan kotak masuk…",13,Color.rgb(166,177,198),false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1,-2); sp.topMargin=dp(12); inboxCard.addView(statusText,sp);
        inboxList = column(); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(8); inboxCard.addView(inboxList,lp);
        TextView warning = text("Catatan: inbox layanan publik dapat dibaca orang lain. Jangan gunakan untuk akun penting, kata sandi, atau informasi pribadi.",12,Color.rgb(255,178,111),false);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1,-2); wp.topMargin=dp(18); page.addView(warning,wp);
        setContentView(scroll);
    }
    // Small drawable wrapper keeps the UI dependency-free.
    private static class GradientDrawableCompat {
        final android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        GradientDrawableCompat(int color,int radius){d.setColor(color);d.setCornerRadius(radius);}
        android.graphics.drawable.Drawable drawable(){return d;}
    }
    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setTextAllCaps(false); b.setTextColor(Color.WHITE);
        b.setBackground(shape(ACCENT,12).drawable()); b.setPadding(dp(8),0,dp(8),0); return b;
    }
    private void generateEmail() {
        statusText.setText("Membuat alamat email…"); emailText.setText("Menghubungkan…");
        executor.execute(() -> {
            try {
                String response = request("https://www.1secmail.com/api/v1/?action=genRandomMailbox&count=1");
                JSONArray arr = new JSONArray(response);
                String generated = arr.getString(0);
                int at = generated.indexOf('@');
                String l = generated.substring(0,at), d = generated.substring(at+1);
                runOnUiThread(() -> {
                    email=generated; login=l; domain=d; expiresAt=System.currentTimeMillis()+3600_000L;
                    emailText.setText(email); statusText.setText("Email siap. Menunggu pesan masuk…");
                    inboxList.removeAllViews(); refreshInbox(false);
                });
            } catch(Exception e) { runOnUiThread(() -> { emailText.setText("Gagal membuat email"); statusText.setText("Layanan tidak tersedia. Coba lagi."); }); }
        });
    }
    private void copyEmail() {
        if (email.isEmpty()) { Toast.makeText(this,"Buat alamat email terlebih dahulu",Toast.LENGTH_SHORT).show(); return; }
        ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Email sementara",email));
        Toast.makeText(this,"Alamat email disalin",Toast.LENGTH_SHORT).show();
    }
    private void refreshInbox(boolean announce) {
        if(email.isEmpty() || loading) return;
        loading=true;
        executor.execute(() -> {
            try {
                String url="https://www.1secmail.com/api/v1/?action=getMessages&login="+URLEncoder.encode(login,"UTF-8")+"&domain="+URLEncoder.encode(domain,"UTF-8");
                JSONArray arr=new JSONArray(request(url));
                runOnUiThread(() -> {
                    inboxList.removeAllViews();
                    if(arr.length()==0) {
                        statusText.setText("Belum ada pesan. Inbox diperiksa otomatis setiap 15 detik.");
                    } else {
                        statusText.setText(arr.length()+" pesan diterima");
                        for(int i=0;i<arr.length();i++) {
                            JSONObject item=arr.optJSONObject(i);
                            final String id=item.optString("id");
                            LinearLayout row=column(); row.setPadding(dp(12),dp(12),dp(12),dp(12)); row.setBackground(shape(Color.rgb(35,44,64),12).drawable());
                            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2); rp.topMargin=dp(7); inboxList.addView(row,rp);
                            row.addView(text(item.optString("from","Pengirim tidak diketahui"),12,Color.rgb(166,177,198),false));
                            TextView subj=text(item.optString("subject","(Tanpa subjek)"),15,Color.WHITE,true);
                            LinearLayout.LayoutParams sjp=new LinearLayout.LayoutParams(-1,-2); sjp.topMargin=dp(5); row.addView(subj,sjp);
                            row.addView(text(item.optString("date",""),11,Color.rgb(166,177,198),false));
                            row.setOnClickListener(v -> readMessage(id));
                        }
                    }
                    loading=false;
                });
            } catch(Exception e) {
                runOnUiThread(() -> { loading=false; statusText.setText("Tidak dapat memuat inbox. Periksa koneksi."); });
            }
        });
    }
    private void readMessage(String id) {
        statusText.setText("Membuka pesan…");
        executor.execute(() -> {
            try {
                String url="https://www.1secmail.com/api/v1/?action=readMessage&login="+URLEncoder.encode(login,"UTF-8")+"&domain="+URLEncoder.encode(domain,"UTF-8")+"&id="+URLEncoder.encode(id,"UTF-8");
                JSONObject m=new JSONObject(request(url));
                String body=m.optString("textBody","");
                if(body.trim().isEmpty()) body=android.text.Html.fromHtml(m.optString("htmlBody","(Pesan kosong)"),android.text.Html.FROM_HTML_MODE_LEGACY).toString();
                final String content=body;
                runOnUiThread(() -> {
                    ScrollView sv=new ScrollView(this); sv.setBackgroundColor(BG);
                    LinearLayout box=column(); box.setPadding(dp(20),dp(20),dp(20),dp(20)); sv.addView(box);
                    Button back=button("← Kembali"); back.setOnClickListener(v -> buildUi());
                    box.addView(text(m.optString("subject","Pesan email"),21,Color.WHITE,true));
                    box.addView(text("Dari: "+m.optString("from",""),13,Color.rgb(166,177,198),false));
                    TextView bodyView=text(content,15,Color.WHITE,false); bodyView.setTextIsSelectable(true);
                    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(18); box.addView(bodyView,p);
                    box.addView(back); setContentView(sv);
                });
            } catch(Exception e) { runOnUiThread(() -> statusText.setText("Gagal membuka pesan.")); }
        });
    }
    private String request(String address) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();
        c.setRequestMethod("GET"); c.setConnectTimeout(12000); c.setReadTimeout(12000);
        c.setRequestProperty("Accept","application/json");
        try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buf=new byte[4096]; int n; while((n=in.read(buf))!=-1)out.write(buf,0,n);
            return out.toString("UTF-8");
        } finally { c.disconnect(); }
    }
    private void updateTimer() {
        if(expiresAt==0) { timerText.setText("Sesi: --:--"); return; }
        long left=Math.max(0,(expiresAt-System.currentTimeMillis())/1000);
        timerText.setText(left==0 ? "Sesi lokal selesai — buat email baru" : String.format(java.util.Locale.US,"Sisa sesi lokal: %02d:%02d",left/60,left%60));
    }
    @Override protected void onDestroy() { handler.removeCallbacks(poll); executor.shutdownNow(); super.onDestroy(); }
}

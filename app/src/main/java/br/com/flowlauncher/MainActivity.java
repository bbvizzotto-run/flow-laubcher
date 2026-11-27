package br.com.flowlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import java.text.Collator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int BG=0xff10151c, PANEL=0xff1b2530, BORDER=0xff33434f, INK=0xfff1f5f8, MUTED=0xffa8b8c6, ACCENT=0xff65d5e4;
    private final ArrayList<App> apps = new ArrayList<>();
    private final ArrayList<String> recent = new ArrayList<>();
    private final String[] sections = {"Hub", "Início", "Aplicativos"};
    private LinearLayout root, nav, appGrid;
    private GestureFrame stage;
    private TextView title, eyebrow;
    private int page=1;
    private SharedPreferences prefs;
    private final Runnable hubObserver = () -> runOnUiThread(() -> { if (page==0 && stage!=null && !isFinishing()) renderPage(false); });
    private static final class App {
        final ResolveInfo info; final ComponentName component; final String label;
        App(ResolveInfo r, PackageManager pm) { info=r; component=new ComponentName(r.activityInfo.packageName,r.activityInfo.name); CharSequence l=r.loadLabel(pm); label=l==null?component.getPackageName():l.toString(); }
        String id() { return component.flattenToString(); }
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("flow",MODE_PRIVATE); loadRecent(); loadApps();
        if(state!=null) page=Math.max(0,Math.min(2,state.getInt("page",1)));
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        if(Build.VERSION.SDK_INT>=29) getWindow().setNavigationBarContrastEnforced(false);
        buildShell(); renderPage(false);
    }
    @Override protected void onResume() { super.onResume(); loadApps(); HubService.observe(hubObserver); renderPage(false); }
    @Override protected void onPause() { HubService.observe(null); super.onPause(); }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); if(intent.hasCategory(Intent.CATEGORY_HOME)) switchPage(1); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putInt("page",page); super.onSaveInstanceState(state); }
    @Override public void onConfigurationChanged(Configuration config) { super.onConfigurationChanged(config); renderPage(false); }
    @Override public void onBackPressed() { if(page!=1) switchPage(1); else if(!isDefaultHome()) super.onBackPressed(); }

    private int dp(float n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private LinearLayout vertical() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout horizontal() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private GradientDrawable bg(int color,int radius,boolean line) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); if(line)d.setStroke(dp(1),BORDER); return d; }
    private TextView text(String s,float size,int color,boolean bold) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private TextView action(String s,Runnable run) { TextView t=text(s,13,ACCENT,true); t.setGravity(Gravity.CENTER); t.setMinHeight(dp(48)); t.setPadding(dp(12),0,dp(12),0); t.setBackground(bg(PANEL,12,true)); t.setOnClickListener(v->run.run()); return t; }
    private void marginAdd(LinearLayout parent,View child,int top) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(top); parent.addView(child,p); }
    private void space(LinearLayout parent,int h) { parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h))); }
    private void buildShell() {
        root=vertical(); root.setBackgroundColor(BG);
        if(Build.VERSION.SDK_INT>=30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((v,i)->{ android.graphics.Insets s=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()); root.setPadding(s.left,s.top,s.right,s.bottom); return i; });
        }
        setContentView(root);
        LinearLayout header=vertical(); header.setPadding(dp(20),dp(12),dp(20),dp(10));
        LinearLayout topline=horizontal(); topline.setGravity(Gravity.CENTER_VERTICAL);
        eyebrow=text("F L O W   /   0 1",11,ACCENT,true); topline.addView(eyebrow,new LinearLayout.LayoutParams(0,-2,1));
        TextView menu=text("⋮",28,INK,true); menu.setGravity(Gravity.CENTER); menu.setContentDescription("Configurações do Flow"); menu.setOnClickListener(v->showSettings()); topline.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48))); header.addView(topline);
        title=text("Início",30,INK,true); header.addView(title); root.addView(header);
        stage=new GestureFrame(); root.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
        nav=horizontal(); nav.setPadding(dp(12),dp(7),dp(12),dp(10)); root.addView(nav,new LinearLayout.LayoutParams(-1,-2));
        root.requestApplyInsets();
    }
    private void switchPage(int target) { if(target<0||target>2||target==page)return; int old=page; page=target; hideKeyboard(); renderPage(true); stage.setTranslationX(dp(old<target?20:-20)); stage.animate().translationX(0).setDuration(160).start(); }
    private void hideKeyboard() { View f=getCurrentFocus(); if(f!=null) { ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(f.getWindowToken(),0); f.clearFocus(); } }
    private void renderPage(boolean changed) {
        if(stage==null)return; stage.removeAllViews(); title.setText(sections[page]); nav.removeAllViews();
        String[] symbols={"◉","▣","▦"};
        for(int i=0;i<3;i++){ final int index=i; LinearLayout tab=vertical(); tab.setGravity(Gravity.CENTER); tab.setPadding(dp(4),dp(7),dp(4),dp(7)); tab.setBackground(bg(i==page?PANEL:BG,12,i==page)); TextView sym=text(symbols[i],21,i==page?ACCENT:MUTED,true); sym.setGravity(Gravity.CENTER); tab.addView(sym); TextView label=text(sections[i],12,i==page?INK:MUTED,i==page); tab.addView(label); tab.setSelected(i==page); tab.setContentDescription(sections[i]); tab.setOnClickListener(v->switchPage(index)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1); p.setMargins(dp(3),0,dp(3),0); nav.addView(tab,p); }
        View content=page==0?hub():page==1?frames():drawer(); stage.addView(content,new FrameLayout.LayoutParams(-1,-1));
    }
    private ScrollView scroll(LinearLayout contents) { ScrollView s=new ScrollView(this); s.setFillViewport(true); s.setClipToPadding(false); s.addView(contents,new ScrollView.LayoutParams(-1,-2)); return s; }
    private LinearLayout body() { LinearLayout l=vertical(); l.setPadding(dp(18),dp(6),dp(18),dp(14)); return l; }
    private void loadApps() {
        PackageManager pm=getPackageManager(); Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        LinkedHashMap<String,App> distinct=new LinkedHashMap<>();
        for(ResolveInfo r:pm.queryIntentActivities(query,0)) { if(r.activityInfo==null||r.activityInfo.packageName.equals(getPackageName()))continue; App a=new App(r,pm); distinct.put(a.id(),a); }
        apps.clear(); apps.addAll(distinct.values()); Collator c=Collator.getInstance(); apps.sort((a,b)->c.compare(a.label,b.label));
    }
    private void loadRecent() { recent.clear(); try { JSONArray arr=new JSONArray(prefs.getString("recent","[]")); for(int i=0;i<arr.length()&&i<12;i++)recent.add(arr.getString(i)); } catch(Exception ignored){} }
    private void saveRecent() { JSONArray arr=new JSONArray(); for(String id:recent)arr.put(id); prefs.edit().putString("recent",arr.toString()).apply(); }
    private App find(String id) { for(App a:apps)if(a.id().equals(id))return a; return null; }
    private App findPackage(String pkg) { for(App a:apps)if(a.component.getPackageName().equals(pkg))return a; return null; }
    private void launch(App a) { try { Intent i=Intent.makeMainActivity(a.component); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED); startActivity(i); recent.remove(a.id()); recent.add(0,a.id()); while(recent.size()>12)recent.remove(recent.size()-1); saveRecent(); } catch(RuntimeException e){ Toast.makeText(this,"Não foi possível abrir o aplicativo.",Toast.LENGTH_SHORT).show(); loadApps(); renderPage(false); } }
    private ImageView icon(App a,int size) { ImageView v=new ImageView(this); try { v.setImageDrawable(a.info.loadIcon(getPackageManager())); } catch(RuntimeException e){v.setImageDrawable(getPackageManager().getDefaultActivityIcon());} v.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size))); v.setScaleType(ImageView.ScaleType.FIT_CENTER); return v; }
    private void appInfo(App a) { try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+a.component.getPackageName()))); } catch(RuntimeException e){Toast.makeText(this,"Configuração indisponível.",Toast.LENGTH_SHORT).show();} }
    private View frames() {
        LinearLayout l=body(); l.addView(text(DateFormat.getDateInstance(DateFormat.FULL).format(new Date()),14,MUTED,false)); space(l,14);
        LinearLayout line=horizontal(); line.setGravity(Gravity.CENTER_VERTICAL); TextView label=text("ACTIVE FRAMES",12,ACCENT,true); line.addView(label,new LinearLayout.LayoutParams(0,-2,1)); TextView clear=text("Limpar",13,MUTED,true); clear.setPadding(dp(10),dp(10),dp(5),dp(10)); clear.setOnClickListener(v->{recent.clear();saveRecent();renderPage(false);}); line.addView(clear); l.addView(line);
        List<App> valid=new ArrayList<>(); for(String id:recent){App a=find(id);if(a!=null)valid.add(a);}
        if(valid.isEmpty()) { LinearLayout empty=vertical(); empty.setPadding(dp(20),dp(24),dp(20),dp(24)); empty.setBackground(bg(PANEL,18,true)); empty.addView(text("Tudo pronto.",23,INK,true)); marginAdd(empty,text("Abra um aplicativo pela grade para vê-lo aqui. Deslize para a esquerda para acessar os aplicativos ou para a direita para o Hub.",15,MUTED,false),10); marginAdd(empty,action("Explorar aplicativos",()->switchPage(2)),18); marginAdd(l,empty,8); }
        int cols=getResources().getConfiguration().screenWidthDp>=700?4:2;
        for(int start=0;start<valid.size();start+=cols){LinearLayout row=horizontal();for(int j=0;j<cols;j++){int idx=start+j;LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(168),1);p.setMargins(dp(4),dp(5),dp(4),dp(5));if(idx>=valid.size()){row.addView(new View(this),p);continue;} App a=valid.get(idx);LinearLayout card=vertical();card.setBackground(bg(PANEL,15,true));card.setPadding(dp(12),dp(9),dp(12),dp(12));LinearLayout top=horizontal();top.setGravity(Gravity.CENTER_VERTICAL);TextView tag=text(idx==0?"ÚLTIMO":"RECENTE",10,ACCENT,true);top.addView(tag,new LinearLayout.LayoutParams(0,-2,1));TextView x=text("×",22,MUTED,false);x.setGravity(Gravity.CENTER);x.setContentDescription("Remover "+a.label+" dos recentes");x.setOnClickListener(v->{recent.remove(a.id());saveRecent();renderPage(false);});top.addView(x,new LinearLayout.LayoutParams(dp(48),dp(48)));card.addView(top);LinearLayout center=vertical();center.setGravity(Gravity.CENTER);center.addView(icon(a,48));card.addView(center,new LinearLayout.LayoutParams(-1,0,1));TextView name=text(a.label,13,INK,true);name.setMaxLines(1);name.setEllipsize(TextUtils.TruncateAt.END);card.addView(name);card.setOnClickListener(v->launch(a));card.setOnLongClickListener(v->{appInfo(a);return true;});row.addView(card,p);}l.addView(row);}
        if(!isDefaultHome())marginAdd(l,action("Definir Flow como tela inicial",this::requestHome),16);
        marginAdd(l,text("Os cartões mostram atalhos recentes do Flow, não janelas em tempo real. Remover um cartão não encerra o processo.",12,MUTED,false),16); return scroll(l);
    }
    private View drawer() {
        LinearLayout l=body(); EditText search=new EditText(this); search.setSingleLine(true); search.setTextColor(INK); search.setHintTextColor(MUTED); search.setHint("Buscar aplicativos"); search.setTextSize(15); search.setPadding(dp(15),dp(10),dp(15),dp(10)); search.setBackground(bg(PANEL,14,true)); l.addView(search,new LinearLayout.LayoutParams(-1,dp(52))); space(l,10);
        appGrid=vertical(); ScrollView s=scroll(appGrid); l.addView(s,new LinearLayout.LayoutParams(-1,0,1)); l.setFocusableInTouchMode(true); l.requestFocus(); populateGrid("");
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){populateGrid(s.toString());}public void afterTextChanged(Editable e){}}); return l;
    }
    private void populateGrid(String query) {
        if(appGrid==null)return;appGrid.removeAllViews();List<App> filtered=new ArrayList<>();String q=query.trim().toLowerCase(Locale.ROOT);for(App a:apps)if(a.label.toLowerCase(Locale.ROOT).contains(q)||a.component.getPackageName().toLowerCase(Locale.ROOT).contains(q))filtered.add(a);
        appGrid.addView(text(filtered.size()+" aplicativos",12,MUTED,false));int cols=Math.max(3,Math.min(8,getResources().getConfiguration().screenWidthDp/90));
        for(int st=0;st<filtered.size();st+=cols){LinearLayout row=horizontal();for(int j=0;j<cols;j++){int idx=st+j;LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(100),1);if(idx>=filtered.size()){row.addView(new View(this),p);continue;}App a=filtered.get(idx);LinearLayout tile=vertical();tile.setGravity(Gravity.CENTER);tile.setPadding(dp(3),dp(4),dp(3),dp(4));tile.addView(icon(a,48));TextView name=text(a.label,12,INK,false);name.setGravity(Gravity.CENTER);name.setMaxLines(2);name.setEllipsize(TextUtils.TruncateAt.END);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(6);tile.addView(name,np);tile.setContentDescription(a.label);tile.setOnClickListener(v->{hideKeyboard();launch(a);});tile.setOnLongClickListener(v->{appInfo(a);return true;});row.addView(tile,p);}appGrid.addView(row);}
    }
    private View hub() {
        LinearLayout l=body();l.addView(text("Sua central de comunicação",15,MUTED,false));space(l,13);
        if(!HubService.connected()){LinearLayout empty=vertical();empty.setPadding(dp(18),dp(18),dp(18),dp(18));empty.setBackground(bg(PANEL,16,true));empty.addView(text("Conectar o Flow Hub",21,INK,true));marginAdd(empty,text("Autorize o acesso às notificações para visualizar mensagens e alertas. Os dados ficam apenas em memória, neste dispositivo. O Flow não acessa diretamente suas contas.",14,MUTED,false),9);marginAdd(empty,action("Autorizar notificações",this::notificationSettings),15);l.addView(empty);return scroll(l);}
        List<HubService.Entry> list=HubService.snapshot();l.addView(text(list.size()+" notificações ativas",12,ACCENT,true));space(l,8);if(list.isEmpty())marginAdd(l,text("Nenhuma notificação no momento.",17,INK,false),20);
        for(HubService.Entry e:list){App a=findPackage(e.pkg);LinearLayout row=horizontal();row.setGravity(Gravity.TOP);row.setPadding(dp(12),dp(12),dp(12),dp(12));row.setBackground(bg(PANEL,12,true));if(a!=null)row.addView(icon(a,34));else {TextView sym=text("◉",24,ACCENT,true);row.addView(sym,new LinearLayout.LayoutParams(dp(34),-2));}LinearLayout content=vertical();LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.leftMargin=dp(10);row.addView(content,cp);String label=a==null?e.pkg:a.label;content.addView(text(label+"  ·  "+DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(e.time)),11,MUTED,false));if(!e.title.isEmpty())marginAdd(content,text(e.title,15,INK,true),4);if(!e.body.isEmpty()){TextView msg=text(e.body,14,INK,false);msg.setMaxLines(4);msg.setEllipsize(TextUtils.TruncateAt.END);marginAdd(content,msg,4);}row.setOnClickListener(v->{try{if(e.action!=null)e.action.send();else if(a!=null)launch(a);else Toast.makeText(this,"Sem ação disponível.",Toast.LENGTH_SHORT).show();}catch(Exception ex){if(a!=null)launch(a);else Toast.makeText(this,"Ação indisponível.",Toast.LENGTH_SHORT).show();}});row.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle("Dispensar notificação?").setMessage("Ela também será removida da central do Android, se o sistema permitir.").setNegativeButton("Cancelar",null).setPositiveButton("Dispensar",(d,w)->HubService.dismiss(e.key)).show();return true;});marginAdd(l,row,7);}
        return scroll(l);
    }
    private void notificationSettings(){try{startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(RuntimeException ex){Toast.makeText(this,"Abra o acesso às notificações nas configurações do Android.",Toast.LENGTH_LONG).show();}}
    private boolean isDefaultHome(){if(Build.VERSION.SDK_INT>=29){RoleManager r=getSystemService(RoleManager.class);if(r!=null&&r.isRoleAvailable(RoleManager.ROLE_HOME))return r.isRoleHeld(RoleManager.ROLE_HOME);}Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);ResolveInfo a=getPackageManager().resolveActivity(i,PackageManager.MATCH_DEFAULT_ONLY);return a!=null&&a.activityInfo!=null&&getPackageName().equals(a.activityInfo.packageName);}
    private void requestHome(){try{if(Build.VERSION.SDK_INT>=29){RoleManager r=getSystemService(RoleManager.class);if(r!=null&&r.isRoleAvailable(RoleManager.ROLE_HOME)&&!r.isRoleHeld(RoleManager.ROLE_HOME)){startActivityForResult(r.createRequestRoleIntent(RoleManager.ROLE_HOME),1);return;}}startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}catch(RuntimeException ex){Toast.makeText(this,"Selecione Flow Launcher em Aplicativos padrão.",Toast.LENGTH_LONG).show();}}
    private void showSettings(){String[] options={"Definir launcher padrão","Acesso às notificações","Configurações do Android","Sobre o Flow Launcher"};new AlertDialog.Builder(this).setTitle("Flow Launcher").setItems(options,(d,w)->{if(w==0)requestHome();else if(w==1)notificationSettings();else if(w==2)startActivity(new Intent(Settings.ACTION_SETTINGS));else new AlertDialog.Builder(this).setTitle("Flow Launcher 0.1.0").setMessage("Protótipo independente inspirado no BlackBerry 10. Sem root, sem internet e sem substituição do SystemUI. Os cartões são atalhos locais. O Hub lê apenas notificações ativas após autorização.").setPositiveButton("OK",null).show();}).show();}
    private final class GestureFrame extends FrameLayout {
        float downX,downY; boolean dragging; final int slop=ViewConfiguration.get(MainActivity.this).getScaledTouchSlop();
        GestureFrame(){super(MainActivity.this);setClickable(true);}
        @Override public boolean onInterceptTouchEvent(MotionEvent e){switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:downX=e.getX();downY=e.getY();dragging=false;return false;case MotionEvent.ACTION_MOVE:float dx=e.getX()-downX,dy=e.getY()-downY;if(Math.abs(dx)>slop*2&&Math.abs(dx)>Math.abs(dy)*1.5f){dragging=true;return true;}break;case MotionEvent.ACTION_CANCEL:case MotionEvent.ACTION_UP:dragging=false;break;default:break;}return false;}
        @Override public boolean onTouchEvent(MotionEvent e){switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:downX=e.getX();downY=e.getY();dragging=false;return true;case MotionEvent.ACTION_MOVE:if(Math.abs(e.getX()-downX)>slop*2)dragging=true;return true;case MotionEvent.ACTION_UP:float dx=e.getX()-downX,dy=e.getY()-downY;if(dragging&&Math.abs(dx)>dp(48)&&Math.abs(dx)>Math.abs(dy)*1.2f)switchPage(page+(dx<0?1:-1));else performClick();dragging=false;return true;case MotionEvent.ACTION_CANCEL:dragging=false;return true;default:return true;}}
        @Override public boolean performClick(){super.performClick();return true;}
    }
}

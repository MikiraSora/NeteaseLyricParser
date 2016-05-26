package com.mycompany.myapp2;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.view.View.*;
import java.io.*;
import java.util.*;
import android.content.*;
import android.widget.AdapterView.*;
import android.opengl.*;
import android.graphics.*;
import java.util.zip.*;
import android.location.*;
import android.text.method.*;

public class MainActivity extends Activity 
{
	LayoutInflater inflater;
	LP_Option opt;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
		opt=new LP_Option();
		opt.handler=new Handler();
		inflater=LayoutInflater.from(getApplicationContext());
		ItemOnClickListener.inflater=inflater;
		ItemOnClickListener.main_layout=(RelativeLayout)findViewById(R.id.main_layout);
		ResultListAdapter adaptet=new ResultListAdapter();
		ResultListAdapter.ctx=getApplicationContext();
		ListView lv=(ListView)findViewById(R.id.main_list);
		lv.setAdapter(adaptet);
	    Button run_button=(Button)findViewById((R.id.run_button));
		opt.ctx=getApplicationContext();
		opt.controller_button=run_button;
		run_button.setOnClickListener(new Run_OnClickListener((RelativeLayout)findViewById((R.id.main_layout)),opt));
		}
		
		public void OnOptionButtonClick(View viewer){
			RelativeLayout rl=(RelativeLayout)inflater.inflate(R.layout.option_layout,null);
			RelativeLayout main=(RelativeLayout)findViewById(R.id.main_layout);
			Button save_button=(Button)rl.findViewById(R.id.save_button);
			save_button.setOnClickListener(new Save_OnClickListener(main,rl,opt));
			
			Spinner _spn=(Spinner)rl.findViewById(R.id.spn_lrc_combine);
			Spinner spn=(Spinner)rl.findViewById(R.id.lrc_type);
			spn.setOnItemSelectedListener(new LPExOnItemSelectedListener(opt,_spn));
			_spn.setOnItemSelectedListener(new LPExOnItemSelectedListener2(opt));
			
			
			main.addView(rl);
			
			((CheckBox)findViewById(R.id.check_extratag)).setChecked(opt.opt.ExtraTag);
			((CheckBox)findViewById(R.id.check_normaltag)).setChecked(opt.opt.NomalTag);
			((CheckBox)findViewById(R.id.check_forcegetlrcfromnet)).setChecked(opt.opt.ForceGetLrcFromNet);
			((CheckBox)findViewById(R.id.check_forcegettagfromnet)).setChecked(opt.opt.ForceGetTagFormNet);

			//TextView tv=(TextView)findViewById(R.id.save_path_text);
			if(opt.save_path.length()!=0)
				((TextView)findViewById(R.id.save_path_text)).setText(opt.save_path);

			if(opt.load_path.length()!=0)
				((TextView)findViewById(R.id.load_path_text)).setText((opt.load_path));
			
		}
		
	public void OnSavePostData(LP_Option _opt){
		opt=_opt;
	}
}

class LPExOnItemSelectedListener2 implements OnItemSelectedListener{
	public LP_Option lp_option;
	public LPExOnItemSelectedListener2(LP_Option op)
	{
		lp_option=op;
	}

	@Override
	public void onItemSelected(AdapterView<?> p1, View p2, int p3, long p4)
	{

		String select_text=(String)p1.getItemAtPosition(p3);

		if(select_text.compareTo("隔行,原版歌词在前")==0){
			lp_option.opt.Lrc_Combine_Type=Option_Lrc_Combine_Type.New_Line_And_Raw_Lrc_First;
		}else{if(select_text.compareTo("隔行,翻译歌词在前")==0){
				lp_option.opt.Lrc_Combine_Type=Option_Lrc_Combine_Type.New_Line_And_Trans_Lrc_First;
			}else{if(select_text.compareTo("并行,原版歌词在前")==0){
					lp_option.opt.Lrc_Combine_Type=Option_Lrc_Combine_Type.Side_By_Side_And_Raw_Lrc_First;
				}else{if(select_text.compareTo("并行,翻译歌词在前")==0){
					  lp_option.opt.Lrc_Combine_Type=Option_Lrc_Combine_Type.Side_By_Side_And_Trans_Lrc_First;
				}}}}
	}

	@Override
	public void onNothingSelected(AdapterView<?> p1)
	{

	}
	
}


class LPExOnItemSelectedListener implements OnItemSelectedListener{
	public LP_Option lp_option;
	public Spinner spn;
	public LPExOnItemSelectedListener(LP_Option op,Spinner _spn)
	{
		lp_option=op;
		spn=_spn;
	}

	@Override
	public void onItemSelected(AdapterView<?> p1, View p2, int p3, long p4)
	{
		
		String select_text=(String)p1.getItemAtPosition(p3);
		
		if(select_text.compareTo("原版歌词")==0){
			spn.setEnabled(false);
			lp_option.opt.Lrc_Type=Option_Lrc_Type.Raw_Lrc;
		}else{if(select_text.compareTo("翻译歌词")==0){
			spn.setEnabled(false);
			lp_option.opt.Lrc_Type=Option_Lrc_Type.Trans_Lrc;
		}else{if(select_text.compareTo("所有歌词")==0){
			spn.setEnabled((true));
			lp_option.opt.Lrc_Type=Option_Lrc_Type.Both_Raw_And_Trans_Lrc;
		}}}
	}

	@Override
	public void onNothingSelected(AdapterView<?> p1)
	{
		
	}

}

class Save_OnClickListener implements OnClickListener{
	RelativeLayout src,back;
	LP_Option  opt;
	public Save_OnClickListener(View _src,View _back,LP_Option _opt){
	src=(RelativeLayout)_src;
	back=(RelativeLayout)_back;
	opt=_opt;
	}
	private Save_OnClickListener()
	{}

	@Override
	public void onClick(View p1)
	{
		opt.save_path=((TextView)back.findViewById(R.id.save_path_text)).getText().toString();
		opt.load_path=((TextView)back.findViewById(R.id.load_path_text)).getText().toString();
		try{
			File f=new File(opt.save_path);
			if(!f.exists()){
				throw new Exception("invaid save files path.");
			}
			f=new File(opt.load_path);
			if(!f.exists()){
				throw new Exception("invaid load files path");
			}
		}catch(Exception e){
			Toast.makeText(opt.ctx,e.getMessage(),Toast.LENGTH_SHORT).show();
			return;
		}
		opt.opt.ForceGetTagFormNet=((CheckBox)back.findViewById(R.id.check_forcegettagfromnet)).isChecked();
		opt.opt.ForceGetLrcFromNet=((CheckBox)back.findViewById(R.id.check_forcegetlrcfromnet)).isChecked();
		opt.opt.NomalTag=((CheckBox)back.findViewById(R.id.check_normaltag)).isChecked();
		opt.opt.ExtraTag=((CheckBox)back.findViewById(R.id.check_extratag)).isChecked();
		opt.controller_button.setEnabled((true));
		src.removeView(back);
	}
	
}

class LP_Option{
	Lrc_Parser_Option opt;
	String save_path;
	Handler handler;
	String load_path;
	Context ctx;
	Button controller_button;
	int thread_count;
	public LP_Option(){
		opt=new Lrc_Parser_Option();
		save_path=new String();
		load_path=new String();
	}
}

class Run_OnClickListener implements OnClickListener{
	RelativeLayout main_layout;
	LP_Option option;
	LPWorkerThreadManager manager;
	//Handler handle;
	
	private Run_OnClickListener(){}
	public Run_OnClickListener(RelativeLayout _main_layout,LP_Option _option){
		option=_option;
		main_layout=_main_layout;
		//handle=h;
	}

	@Override
	public void onClick(View p1)
	{
		p1.setEnabled(false);
		manager=new LPWorkerThreadManager(option.save_path,option.load_path,option.opt,main_layout,option.handler);
		manager.start();
	}
}

class LPWorkerThreadManager extends Thread{
	Lrc_Parser_Option option;
	RelativeLayout displayer;
	String save_path,load_path;
	Handler handler;
	/*
	Button controller;
	Context ctx;*/
	public LPWorkerThreadManager(String _save_path,String _load_path,Lrc_Parser_Option _option,RelativeLayout _displayer,Handler h){
		LPWorkerThread.alive_count=1;
		displayer=_displayer;
		save_path=_save_path;
		load_path=_load_path;
		option=_option;
		handler=h;
	}

	@Override
	public void run(){
		
		File path=new File(load_path);
		
		LPWorkerThread t;
		File[] files=path.listFiles();
		System.gc();
		for(File f:files){		
			while(LPWorkerThread.alive_count<=0){
				continue;
			}
			if(f.isDirectory())
				continue;
			LPWorkerThread.alive_count--;
			t=new LPWorkerThread(save_path,f.getAbsolutePath(),option,displayer,handler);
			
			t.start();
		}
	}
	
}

class LPWorkerThread extends Thread{
	Lrc_Parser_Option option;
	RelativeLayout displayer;
	String save_path,load_path;
	Handler handler;
	
	public static int alive_count=1;
	
	private LPWorkerThread(){}
	public LPWorkerThread(String _save_path,String _load_path,Lrc_Parser_Option _option,RelativeLayout _displayer,Handler h){
		displayer=_displayer;
		save_path=_save_path;
		load_path=_load_path;
		option=_option;
		handler=h;
	}

	@Override
	public void run()
	{
		//alive_count--;
		try{
            
			FileInputStream file=new FileInputStream(load_path);
			Reader r=new InputStreamReader(file);
			CharSequence chars="";
			int c;
			while((c=r.read())!=-1)
				chars+=String.valueOf((char)c);
			file.close();

			Lrc_Parser lp=new Lrc_Parser(option,new Lrc_Parser_Expr());
			System.out.println();
			Lrc_Parser_Result res=lp.Decode(chars.toString(),option);

			File outFile=new File(save_path+res.Artist+" - "+res.Title+".lrc");
			outFile.createNewFile();
			FileOutputStream os=new FileOutputStream(outFile.getAbsolutePath());
			Writer w=new OutputStreamWriter(os);
			w.write(res.Lyric);
			w.close();
			
			//Handler handler=new Handler();
			ResultUpdateRunnable updater=new ResultUpdateRunnable(res,displayer,outFile.getAbsolutePath());
			handler.post(updater);
		}catch(Exception e){
			e.fillInStackTrace();
			System.out.println(e);
			}finally{
				alive_count++;
			}

	    super.run();
		//alive_count++;
	}

}

class ResultUpdateRunnable implements Runnable{
	Lrc_Parser_Result result;
	RelativeLayout displayer;
	String abs_path;
	private ResultUpdateRunnable(){}
	public ResultUpdateRunnable(Lrc_Parser_Result _result,RelativeLayout _displayer,String _abs_path){
		displayer=_displayer;
		result=_result;
		abs_path=_abs_path;
	}
	
	@Override
	public void run()
	{
		String text=new String();
		ResultListAdapter adapter=(ResultListAdapter)((ListView)displayer.findViewById(R.id.main_list)).getAdapter();
		text+=(adapter.getCount()+1)+" :("+result.id+") "+result.Artist+" - "+result.Title;
		adapter.addItem(text,result.Artist+" - "+result.Title,abs_path);
	}

}

class ResultListAdapter extends BaseAdapter{
	public static Context ctx;
	ArrayList<Item> list;
	Random rand;
	public ResultListAdapter()
	{
		list = new ArrayList<Item>();
		rand = new Random();
	}
	
	public void addItem(String text,String SongName,String abs_path){
		Item i=new Item();
		i.color=Color.rgb(rand.nextInt(255),rand.nextInt(255),rand.nextInt(255));
		i.text=text;
		i.abs_full_path=abs_path;
		i.Songname=SongName;
		list.add(i);
		notifyDataSetChanged();
	}

	@Override
	public long getItemId(int p1)
	{
		// TODO: Implement this method
		return p1;
	}

	@Override
	public int getCount()
	{
		// TODO: Implement this method
		return list.size();
	}

	@Override
	public Object getItem(int p1)
	{
		// TODO: Implement this method
		return list.get(p1);
	}

	@Override
	public View getView(int p1, View p2, ViewGroup p3)
	{
		TextView tv=null;
		if(p2==null){
			tv=new TextView(ctx);
		}else{
			tv=(TextView)p2;
		}
		Item i=list.get(p1);
		tv.setTextColor(i.color);
		//tv.setTextSize(13);
		tv.setText(i.text);	
		//tv.setTextColor(Color.rgb(0,0,0));
		tv.setTag(i);
		tv.setOnClickListener(new ItemOnClickListener());
		return tv;
		}
		
}

class Item{
	int color;
	String text,Songname,abs_full_path;
}

class ItemOnClickListener implements OnClickListener{
	public static RelativeLayout main_layout;
	public static LayoutInflater inflater;
	public static RelativeLayout lyric_layout;
	@Override
	public void onClick(View p1)
	{
		if(lyric_layout!=null)
			return;
		lyric_layout=(RelativeLayout)inflater.inflate(R.layout.lyric,null);
		try{
			FileInputStream file=new FileInputStream(((Item)p1.getTag()).abs_full_path);
			Reader r=new InputStreamReader(file);
			CharSequence chars="";
			int c;
			while((c=r.read())!=-1)
				chars+=String.valueOf((char)c);
			file.close();
			main_layout.addView(lyric_layout);
			((TextView)lyric_layout.findViewById(R.id.lrc_text)).setText(chars);
			((TextView)lyric_layout.findViewById(R.id.lrc_text)).setTextColor(Color.rgb(0,0,0));
			((TextView)lyric_layout.findViewById(R.id.lrc_text)).setMovementMethod(ScrollingMovementMethod.getInstance());
			lyric_layout.setAlpha(0.85f);
			((Button)lyric_layout.findViewById(R.id.lyric_back)).setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View h){
					main_layout.removeView(lyric_layout);
					lyric_layout=null;
				}
			});
			((TextView)lyric_layout.findViewById(R.id.file_name_text)).setText(((Item)p1.getTag()).Songname);
		}catch(Exception e){
			e.fillInStackTrace();
		}
	}
}

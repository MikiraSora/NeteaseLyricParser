package com.mycompany.myapp2; 

import java.util.*;
import java.net.*;
import java.io.*;
import java.util.regex.*;

class Lrc_Parser_Result
{
	HashMap<String,String> data;
	String Title,Album,Artist,Lyric;
	int id;
	boolean is_Finish_Parse;
}

class Lrc_Parser_Info
{
	String Artist=null,Title=null,Album=null;
}

enum Option_Lrc_Type
{
	Raw_Lrc,
	Trans_Lrc,
	Both_Raw_And_Trans_Lrc
	}

enum Option_Lrc_Combine_Type
{
	New_Line_And_Raw_Lrc_First,
	New_Line_And_Trans_Lrc_First,
	Side_By_Side_And_Raw_Lrc_First,
	Side_By_Side_And_Trans_Lrc_First
	}


class Lrc_Parser_Option
{
	String Tmp_Path="/sdcard/Lrc_Parser_Tmp/";
	boolean ExtraTag = true;
	boolean NomalTag = true;
	boolean ForceGetTagFormNet = false;
	boolean ForceGetLrcFromNet=false;
	boolean NotToGetTagFromNet=false;
	boolean NotToGetLrcFromNet=false;
	Option_Lrc_Type Lrc_Type = Option_Lrc_Type.Both_Raw_And_Trans_Lrc;
	Option_Lrc_Combine_Type Lrc_Combine_Type =
	Option_Lrc_Combine_Type.New_Line_And_Raw_Lrc_First;
}

class Lrc_Parser_Expr
{
	int id=0;

	String expr_split_lrc="(\\[\\d{2}\\d*\\:\\d{2}(\\.\\d*)?\\])(.*)";
	int lrc_split_id=3;
	String expr_lrc="(\\[\\d{2}\\d*\\:\\d{2}(\\.\\d*)?\\](\\s*.*?))(?=\\s*\\\\n)";
	int lrc_id = 1; 
	String expr_tag="\\[\\s*([^\\d]+?)\\s*\\:\\s*(.+?)\\s*\\]";
	int tag_name_id=1;
	int tag_value_id=2;
	String expr_lrc_time="\\[(\\d{2}\\d*)\\:(\\d{2})(\\.(\\d*))?\\]";
	int lrc_time_min=1;
	int lrc_time_sec=2;
	int lrc_time_msec=4;
	String expr_online_info="(<title>)((.+?)\\s-\\s(.+?))((?=（)|(?=\\s*-?\\s*网易云音乐)|(?=</title>))";
	int online_title_id=3;
	int online_artist_id=4;
	String expr_data="\"\\s*([\\w\\d\"-]+)\"\\s*\\:\\s*(((\"\")|(\"(.+?)\")|((-?[\\d\\w]+)))(?=(\\})|(\\,\"\\s*([\\w\\d\"-]+))\"\\s*\\:))";
	int sub_data_name_id = 1;
	int sub_data_value_id = 2; // or 3

	String expr_info_name="data\\-res\\-name\\=\"(.+)\"";
	int info_name_id=1;
	String expr_info_artist="data-res-author=\"(.+)\"";
	int info_artist_id=1;
	//boolean LoadExprFormFile(String absolute_path){return false;}

}

class Lrc_Parser{

	String last_result=null;
	HashMap < String, String > data=new HashMap<String,String>();

	ArrayList < String > raw_lrc=new ArrayList<String>();
	ArrayList<String>trans_lrc=new ArrayList<String>();
	ArrayList<String>lrc=new ArrayList<String>();

	Lrc_Parser_Option option=new Lrc_Parser_Option();
	Lrc_Parser_Expr  expr=new Lrc_Parser_Expr();

	private Lrc_Parser(){}
	public Lrc_Parser(Lrc_Parser_Option o,Lrc_Parser_Expr e){
		if(o!=null)
			option=o;
		if(e!=null)
			expr=e;
	}

	private boolean GetLrcFromNet(int id){
		String buf=new String();	
		String addr="http://music.163.com/api/song/media?id="+id;
		try{
			URL url = new URL(addr);
			HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();

			String response = httpCon.getResponseMessage();
			buf+=("HTTP/1.x " + httpCon.getResponseCode() + " " + response + "\n");
			InputStream in = new BufferedInputStream(httpCon.getInputStream());
			Reader r = new InputStreamReader(in);
			int c;
			while ((c = r.read()) != -1) {
				buf+=(String.valueOf((char) c));
			}
			in.close();
		}catch(Exception e){
			e.fillInStackTrace();
			return false;
		}
		ParserLrc(buf,true);
		return ((raw_lrc.size()>0)?true:false);
	}
		
	private boolean GetLrcFromNet_tv(int id){
		String buf=new String();	
		String addr="http://music.163.com/api/song/lyric?id="+id+"&tv=-1";
		try{
			URL url = new URL(addr);
			HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
			
			/*vector<pair<string,string>>({pair<string,string>("Cookie","appver=3.1.4"),
			 pair<string,string>("Referer","http://music.163.com")})*/
			httpCon.addRequestProperty("Cookie","appver=3.1.4");
			httpCon.addRequestProperty("Referer","http://music.163.com");
			
			String response = httpCon.getResponseMessage();
			buf+=("HTTP/1.x " + httpCon.getResponseCode() + " " + response + "\n");
			InputStream in = new BufferedInputStream(httpCon.getInputStream());
			Reader r = new InputStreamReader(in);
			int c;
			while ((c = r.read()) != -1) {
				buf+=(String.valueOf((char) c));
			}
			in.close();
		}catch(Exception e){
			e.fillInStackTrace();
			return false;
		}

		try{
			if(buf.length()==0){
				Exception e=new Exception("Cant get html from net :"+addr);
				e.printStackTrace();
				throw e;
			}
		}catch(Exception e){
			//haha
		}
		ParserLrc(buf,false);
		return ((trans_lrc.size()>0)?true:false);
	}


	private Lrc_Parser_Info GetTagFromNet(int id,String _url)throws Exception{
		String buf=new String();
		Lrc_Parser_Info info=new Lrc_Parser_Info();
		String addr=null;
		if(id>0)
			addr="http://music.163.com/m/song/"+id+"/?userid=0";
		else
			addr=_url;
			URL url = new URL(addr);
			HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
			
			httpCon.setConnectTimeout(30000);
			String response = httpCon.getResponseMessage();
			buf+=("HTTP/1.x " + httpCon.getResponseCode() + " " + response + "\n");
			
			InputStream in = new BufferedInputStream(httpCon.getInputStream());
			Reader r = new InputStreamReader(in);
			int c;
			while ((c = r.read()) != -1) {
				buf+=(String.valueOf((char) c));
			}
			in.close();
		

			if(buf.length()==0){
				Exception e=new Exception("Cant get html from net :"+addr);
				e.printStackTrace();
				throw e;
			}
		
			//haha
		
    	Pattern reg=Pattern.compile(expr.expr_info_name);
		Matcher result=reg.matcher(buf);
		int count=0;
		while(result.find()){
			for(int i=0;i<=result.groupCount();i++){
				if(i==expr.info_artist_id){
					info.Artist=result.group();
					continue;
				}
				if(i==expr.info_name_id){
					info.Title=result.group();
				}
			}
			count++;
		}
		
		if(info.Title==null||info.Artist==null){
			reg=Pattern.compile(expr.expr_online_info);
			result=reg.matcher(buf);
			while(result.find()){
				info.Title=result.group(expr.online_title_id);
				info.Artist=result.group(expr.online_artist_id);
				count++;
			}
		}
		//
		return info;
	}

	private void ParserLrc(String buffer,boolean isRaw){
		ArrayList<String> m=(isRaw?raw_lrc:trans_lrc);
		Pattern reg=Pattern.compile(expr.expr_lrc);
		Matcher result=reg.matcher(buffer);
		int l_id=expr.lrc_id;
		while(result.find()){
			m.add(result.group(l_id));
		}
	}

	private void ParserData(String buffer){
		int n_id=expr.sub_data_name_id;
		int v_id=expr.sub_data_value_id;
	
		Pattern reg=Pattern.compile(expr.expr_data);
		Matcher result=reg.matcher(buffer);
		while(result.find()){
			data.put(result.group(n_id),result.group(v_id));
		}
	}

	private void ParserNomalTag(String buffer,Lrc_Parser_Option opt)throws Exception{
		Lrc_Parser_Info info=null;

		int n_id=expr.tag_name_id,v_id=expr.tag_value_id;
		Pattern reg=Pattern.compile(expr.expr_tag);
		Matcher result=reg.matcher(buffer);
		while(result.find()){
			data.put(result.group(n_id),result.group(v_id));
		}
		int _id=-1;
		String weburl="";

		boolean hasTag=(data.containsKey("ti")&&data.containsKey("ar"));
		if(((!(hasTag))||opt.ForceGetTagFormNet)){
    	    if(data.containsKey("musicId")){
				_id=Integer.parseInt(data.get("musicId"));
			}else{ if(data.containsKey("#")){
					weburl=data.get("#");
    	        }else{
					weburl="N/A";
				}

			}
			if(!opt.NotToGetTagFromNet) 
				info=GetTagFromNet(_id,weburl); 
		    
				if(info==null)
					throw new Exception("-Cant get info from GetTagFromNet()");
			

			//check_p("save info value");
			data.put("ti",info.Title);
			data.put("ar",info.Artist);
		}
		//check_p("get text is finish");
	}

	private long CoverLrcTime(String str){
		Pattern reg=Pattern.compile(expr.expr_lrc_time);
		Matcher result=reg.matcher(str);
		while(result.find()){
			long min=Long.parseLong(result.group(expr.lrc_time_min)),sec=Long.parseLong(result.group(expr.lrc_time_sec)),msec=Long.parseLong(result.group(expr.lrc_time_msec));
			return min*60000+sec*1000+msec;
		}
		return -1;
	}

	private void CombineLrc(Lrc_Parser_Option opt){
		ArrayList<String> _f=null,_a=null;
		if((raw_lrc.size()!=0)&&(trans_lrc.size()==0)){
			lrc=raw_lrc;
			// printf("\nonly have raw lrc\n");
			return ;
		}
		if((trans_lrc.size()!=0)&&(raw_lrc.size()==0)){
			lrc=(trans_lrc);
			//printf("\nonly have tans lrc %d\n",trans_lrc.size());
			return;
		}
		try{
			if(!(trans_lrc.size()!=0||raw_lrc.size()!=0))
				throw new Exception("Combine_Lrc() : No any lrc in trans_lrc or raw_lrc.");
		}catch(Exception e){
			e.fillInStackTrace();
		}
		// printf("\nboth two version lrc have.\n");
		boolean isNewLine=true;
		switch(opt.Lrc_Combine_Type){
			case New_Line_And_Raw_Lrc_First:
				_f=raw_lrc;
				_a=trans_lrc;
				isNewLine=true;
				break;

			case New_Line_And_Trans_Lrc_First:
				_f=trans_lrc;
				_a=raw_lrc;
				isNewLine=true;
				break;

			case Side_By_Side_And_Raw_Lrc_First:
				_f=raw_lrc;
				_a=trans_lrc;
				isNewLine=false;
				break;

			case Side_By_Side_And_Trans_Lrc_First:
				_f=trans_lrc;
				_a=raw_lrc;
				isNewLine=false;
				break;

			default:
				//throw new Exception("Combine_Lrc() : unknown combine type");
		}

		if(isNewLine){
			if(_f!=null)
				for(String i :_f)
					lrc.add(i);
			if(_a!=null)
				for(String i :_a)
					lrc.add(i);
			System.out.println();
		}else{

			//time -> [time]_f_lrc + _a_lrc
			HashMap<Long,String> r_lrc=new HashMap<Long,String>();
			/*smatch sm;
			 regex reg(expr->expr_split_lrc);*/
			Pattern reg=Pattern.compile((expr.expr_split_lrc));
			Matcher result=null;
			long   _t=-1;
			String _i,_u=null,_o;
			int _c=0;
			for(int i=0;i<_f.size();i++){
				_i=_f.get(i);
				_t=CoverLrcTime(_i);
				r_lrc.put(_t,_i);
				// printf("\n%d",_t);
				_c++;
			}
			//printf("\n has %d lrc save into map,now last list has %d lrc",r_lrc.size(),_a.size());
			for(int i=0;i<_a.size();i++){
				_i=_a.get(i);
				_t=CoverLrcTime(_i);
				if(!r_lrc.containsKey(_t)){
					r_lrc.put(_t,_i);
					//printf("\ntime %d lrc not found!",_t);
					continue;
				}
				/*
				 if(!regex_search(_i,sm,reg)){
				 // printf("\ntime %d - %s cant match",_t,_i.c_str());
				 continue;  	   List<             
				 }*/
				result=reg.matcher(_i);
				while(result.find()){
					_u=result.group(expr.lrc_split_id);
				}
				
				_o=r_lrc.get(_t);
				_o+="/"+_u;
				r_lrc.put(_t,_o);
			}
			
			List<Map.Entry<Long,String>> list=new ArrayList<Map.Entry<Long,String>>(r_lrc.entrySet());
			
			Collections.sort(list,new Comparator<Map.Entry<Long,String>>(){
				@Override
				public int compare(Map.Entry<Long,String> k1,Map.Entry<Long,String> k2){
					return (int)((Long)(k1.getKey()) - (Long)(k2.getKey()));
					
				}
			});
			
			for(Map.Entry<Long,String> _d : list){
				lrc.add(_d.getValue());
			}
			return;    	           
		}
	}


	public Lrc_Parser_Result Decode(String text,Lrc_Parser_Option opt)throws Exception{
		opt=((opt!=null)?opt:option);
		ParserData(text);

		if(0==data.size())
			throw new Exception("Decode() : No data in map");
		ParserNomalTag(text,opt);
		int _id=Integer.parseInt(data.get("musicId"));

        if(!opt.NotToGetLrcFromNet){
            if(opt.ForceGetLrcFromNet){
                GetLrcFromNet(_id);
			}else{
				if(opt.Lrc_Type==Option_Lrc_Type.Raw_Lrc){

				}
				if(data.containsKey("lyric")&&((opt.Lrc_Type==Option_Lrc_Type.Raw_Lrc)||(opt.Lrc_Type==Option_Lrc_Type.Both_Raw_And_Trans_Lrc))){
					ParserLrc(data.get("lyric"),true);   
				}
            }                                               

		}
        else{
			if(data.containsKey("lyric")&&((opt.Lrc_Type==Option_Lrc_Type.Raw_Lrc)||(opt.Lrc_Type==Option_Lrc_Type.Both_Raw_And_Trans_Lrc))){
				ParserLrc(data.get("lyric"),true);
				if(raw_lrc.size()==0){
					if(!opt.NotToGetLrcFromNet){ 
						GetLrcFromNet( _id);    	                  }

				}
			}

		}
		
		////////
		if(!opt.NotToGetLrcFromNet){
            if(opt.ForceGetLrcFromNet){
                GetLrcFromNet_tv(_id);
			}else{
				if(opt.Lrc_Type==Option_Lrc_Type.Trans_Lrc){

				}
				if(data.containsKey("translateLyric")&&((opt.Lrc_Type==Option_Lrc_Type.Trans_Lrc)||(opt.Lrc_Type==Option_Lrc_Type.Both_Raw_And_Trans_Lrc))){
					ParserLrc(data.get("translateLyric"),false);   
				}
            }                                               

		}
        else{
			if(data.containsKey("translateLyric")&&((opt.Lrc_Type==Option_Lrc_Type.Trans_Lrc)||(opt.Lrc_Type==Option_Lrc_Type.Both_Raw_And_Trans_Lrc))){
				ParserLrc(data.get("translateLyric"),false);
				if(trans_lrc.size()==0){
					if(!opt.NotToGetLrcFromNet){ 
						GetLrcFromNet_tv( _id);    	                  }

				}
			}

		}
		///////
		//ParserLrc(data.get("translateLyric"),false);

		if(!(raw_lrc.size()!=0||trans_lrc.size()!=0))
			throw new Exception("Decode() : Cant got any lrc from file");
		
		CombineLrc(opt);
	
		if(lrc.size()==0)  	      
			throw new Exception("Decode() : output lrc_list hasnt any lrc");

		add_ex_info(opt,"#");
		add_ex_info(opt,"musicId");
		add_nm_info(opt,"by");
		add_nm_info(opt,"al");
		add_nm_info(opt,"co");
		add_nm_info(opt,"ar");
		add_nm_info(opt,"ti");
		add_nm_info(opt,"lr");
		
		String _lrc=new String();
		for(String it : lrc){
			if(it!=null)
				_lrc+=(it)+("\n");
		}    	        	    
		if(_lrc.length()==0)
			throw new Exception("Decode() : no lrc add in.");

		Lrc_Parser_Result r=new Lrc_Parser_Result();
		//#define add_info(y,x) 
		//r->Title=data["ti"];
		if(data.containsKey("ti")){r.Title=data.get("ti");}
		if(data.containsKey("ar")){r.Artist=data.get("ar");}
		if(data.containsKey("al")){r.Album=data.get("al");}
		/*add_info(Title,"ti")
		 add_info(Artist,"ar")
		 add_info(Album,"al")*/

		r.id=(Integer.parseInt(data.get("musicId")));
		//add_info(
		r.Lyric=_lrc;
		//#undef add_info
        if(r.Lyric==null||r.Title==null||r.Artist==null)
			throw new Exception("Decode() : Had some variables are null in Result.");
		r.is_Finish_Parse=((r.Lyric.length()!=0)&&(r.Title.length()!=0)&&(r.Artist.length()!=0));

		return r;
	}

	private void add_ex_info(Lrc_Parser_Option opt,String ro){
		if(opt.ExtraTag)
			add_tag(ro);
	}

	private void add_nm_info(Lrc_Parser_Option opt,String ro){
		if(opt.NomalTag)
			add_tag((ro));
	}

	private void add_tag(String tag_name){
		if(data.containsKey(tag_name)){String _s=new String();_s+="["+(tag_name)+(":")+data.get(tag_name)+("]");lrc.add(0,_s);}
	}

	String GetLastDecodeResult(){return last_result;}
}

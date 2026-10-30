package com.subn3xt.studio

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val Ink=Color(0xFF0B1020); private val Panel=Color(0xFF151D31); private val Violet=Color(0xFF8B7CFF); private val Mint=Color(0xFF54E0C1)
data class MediaProject(val id:Long,val name:String,val uri:String)
data class Cue(val start:String="00:00:00,000",val end:String="00:00:03,000",val text:String="")

class MainActivity:ComponentActivity(){ override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{SubN3xtApp(this)}} }

@Composable fun SubN3xtApp(ctx:Context){
 var projects by remember{mutableStateOf(loadProjects(ctx))}; var selected by remember{mutableStateOf<MediaProject?>(null)}
 var tab by remember{mutableStateOf("Home")}; var cues by remember{mutableStateOf(listOf(Cue()))}; var notice by remember{mutableStateOf("")}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){ uri:Uri? ->
  if(uri!=null){try{ctx.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){}
   val p=MediaProject(System.currentTimeMillis(),uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?:"Imported media",uri.toString())
   projects=projects+p;saveProjects(ctx,projects);selected=p;cues=listOf(Cue());tab="Editor";notice="Media imported"}
 }
 val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")){uri:Uri? ->
  if(uri!=null)try{val s=cues.mapIndexed{i,c->"${i+1}\n${c.start}\n${c.end}\n${c.text}\n"}.joinToString("\n");ctx.contentResolver.openOutputStream(uri)?.use{it.write(s.toByteArray(Charsets.UTF_8))};notice="SRT exported"}catch(e:Exception){notice="Export error: ${e.message}"}}
 MaterialTheme(colorScheme=darkColorScheme(background=Ink,surface=Panel,primary=Violet,secondary=Mint,onSurface=Color.White)){
  Scaffold(containerColor=Ink,bottomBar={NavigationBar(containerColor=Panel){listOf("Home","Projects","Editor","Settings").forEach{item->val icon=when(item){"Home"->Icons.Rounded.Home;"Projects"->Icons.Rounded.VideoLibrary;"Editor"->Icons.Rounded.Subtitles;else->Icons.Rounded.Settings};NavigationBarItem(selected=tab==item,onClick={tab=item},icon={Icon(icon,item)},label={Text(item,fontSize=10.sp)})}}}){pad->
   when(tab){
    "Home"->HomeScreen(Modifier.padding(pad),projects.size){picker.launch(arrayOf("video/*","audio/*"))}
    "Projects"->ProjectsScreen(Modifier.padding(pad),projects,{p->selected=p;cues=listOf(Cue());tab="Editor"},{picker.launch(arrayOf("video/*","audio/*"))},{p->projects=projects.filterNot{it.id==p.id};saveProjects(ctx,projects);if(selected?.id==p.id)selected=null})
    "Editor"->EditorScreen(Modifier.padding(pad),selected,cues,{cues=it},{picker.launch(arrayOf("video/*","audio/*"))},{exporter.launch((selected?.name?:"subtitles").substringBeforeLast('.')+".srt")})
    else->Column(Modifier.padding(pad).fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.Settings,null,tint=Mint,modifier=Modifier.size(48.dp));Text("Test build",fontSize=24.sp);Text("Local projects • Manual subtitle editing • SRT export",color=Color.LightGray)}
   }
  }
  if(notice.isNotBlank()){LaunchedEffect(notice){kotlinx.coroutines.delay(2200);notice=""};Surface(color=Color(0xFF26334D),shape=RoundedCornerShape(12.dp),modifier=Modifier.padding(12.dp)){Text(notice,color=Color.White,modifier=Modifier.padding(14.dp))}}
 }
}
private fun loadProjects(c:Context):List<MediaProject> = try{val a=JSONArray(c.getSharedPreferences("subn3xt",0).getString("projects","[]"));(0 until a.length()).map{val o=a.getJSONObject(it);MediaProject(o.getLong("id"),o.getString("name"),o.getString("uri"))}}catch(_:Exception){emptyList()}
private fun saveProjects(c:Context,ps:List<MediaProject>){val a=JSONArray();ps.forEach{a.put(JSONObject().put("id",it.id).put("name",it.name).put("uri",it.uri))};c.getSharedPreferences("subn3xt",0).edit().putString("projects",a.toString()).apply()}

@Composable fun HomeScreen(modifier:Modifier,count:Int,onImport:()->Unit){
 LazyColumn(modifier.fillMaxSize().padding(20.dp),contentPadding=PaddingValues(top=20.dp,bottom=24.dp)){item{
 Text("SUBN3XT",color=Mint,fontWeight=FontWeight.Bold,letterSpacing=3.sp);Text("Studio",fontSize=34.sp,fontWeight=FontWeight.ExtraBold);Text("Subtitle workspace for Android",color=Color.LightGray)
 Spacer(Modifier.height(24.dp));Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF29285A),Color(0xFF172E45))),RoundedCornerShape(24.dp)).padding(22.dp)){
 Icon(Icons.Rounded.AutoAwesome,null,tint=Mint);Spacer(Modifier.height(12.dp));Text("Turn media into",fontSize=25.sp,fontWeight=FontWeight.Bold);Text("beautiful subtitles.",fontSize=25.sp,fontWeight=FontWeight.Bold);Text("Import • Write • Export SRT",color=Color.LightGray)
 Spacer(Modifier.height(18.dp));Button(onClick=onImport,modifier=Modifier.fillMaxWidth()){Icon(Icons.Rounded.Add,null);Text("  Import video or audio")}
 }
 Spacer(Modifier.height(22.dp));Text("PROJECTS  $count",color=Mint,fontWeight=FontWeight.Bold);Spacer(Modifier.height(14.dp));Text("1  Import a media file");Text("2  Add subtitle text and timestamps");Text("3  Export a standard .srt file");Spacer(Modifier.height(16.dp));Text("AI transcription is not included in this test build.",color=Color(0xFFFFC879),fontSize=12.sp)
 }}
}
@Composable fun ProjectsScreen(modifier:Modifier,ps:List<MediaProject>,open:(MediaProject)->Unit,add:()->Unit,delete:(MediaProject)->Unit){
 Column(modifier.fillMaxSize().padding(18.dp)){Text("Projects",fontSize=30.sp,fontWeight=FontWeight.Bold);Text("${ps.size} saved projects",color=Color.LightGray);Button(onClick=add,modifier=Modifier.fillMaxWidth().padding(vertical=12.dp)){Icon(Icons.Rounded.Add,null);Text(" Import media")}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(ps,key={it.id}){p->Surface(color=Panel,shape=RoundedCornerShape(14.dp)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.Movie,null,tint=Mint);Spacer(Modifier.width(10.dp));Text(p.name,color=Color.White,modifier=Modifier.weight(1f));IconButton(onClick={open(p)}){Icon(Icons.Rounded.Edit,null,tint=Mint)};IconButton(onClick={delete(p)}){Icon(Icons.Rounded.DeleteOutline,null,tint=Color.Red)}}}}}
 }
}
@Composable fun EditorScreen(modifier:Modifier,p:MediaProject?,cues:List<Cue>,change:(List<Cue>)->Unit,addMedia:()->Unit,export:()->Unit){
 Column(modifier.fillMaxSize().padding(16.dp)){Text("Subtitle Editor",fontSize=27.sp,fontWeight=FontWeight.Bold)
 if(p==null){Text("Select a project or import media.",color=Color.LightGray);Button(onClick=addMedia){Text("Import media")}}
 else{Text(p.name,color=Mint,maxLines=2);Text("Edit timestamps in HH:MM:SS,mmm format.",fontSize=12.sp,color=Color.LightGray)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(vertical=10.dp)){OutlinedButton(onClick={change(cues+Cue())},modifier=Modifier.weight(1f)){Icon(Icons.Rounded.Add,null);Text(" Add line")};Button(onClick=export,modifier=Modifier.weight(1f)){Icon(Icons.Rounded.FileDownload,null);Text(" Export SRT")}}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(cues.size){i->val c=cues[i];Surface(color=Panel,shape=RoundedCornerShape(14.dp)){Column(Modifier.padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("LINE ${i+1}",color=Mint,modifier=Modifier.weight(1f));IconButton(onClick={if(cues.size>1)change(cues.filterIndexed{j,_->j!=i})}){Icon(Icons.Rounded.Close,null,tint=Color.Red)}};Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(c.start,{v->change(cues.mapIndexed{j,x->if(j==i)x.copy(start=v)else x})},label={Text("Start")},modifier=Modifier.weight(1f),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii));OutlinedTextField(c.end,{v->change(cues.mapIndexed{j,x->if(j==i)x.copy(end=v)else x})},label={Text("End")},modifier=Modifier.weight(1f),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii))};OutlinedTextField(c.text,{v->change(cues.mapIndexed{j,x->if(j==i)x.copy(text=v)else x})},label={Text("Subtitle text")},modifier=Modifier.fillMaxWidth(),minLines=2)}}}}
 }
 }
}

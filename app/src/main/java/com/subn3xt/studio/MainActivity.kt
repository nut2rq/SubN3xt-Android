package com.subn3xt.studio

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val Ink=Color(0xFF0B1020); private val Panel=Color(0xFF151D31); private val Violet=Color(0xFF8B7CFF); private val Mint=Color(0xFF54E0C1)
private const val MODEL_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin?download=true"
data class MediaProject(val id:Long,val name:String,val uri:String)

class MainActivity:ComponentActivity(){ override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{SubN3xtApp(this)}} }

private fun displayName(ctx:Context,uri:Uri):String = try {
 ctx.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { c -> if(c.moveToFirst()) c.getString(0) else null } ?: "Imported audio"
} catch(_:Exception) { "Imported audio" }

private suspend fun copyUriToCache(ctx:Context,uri:Uri):File = withContext(Dispatchers.IO) {
 val out=File(ctx.cacheDir,"subn3xt_input_"+System.currentTimeMillis()+".audio")
 ctx.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } } ?: throw IllegalStateException("Cannot read selected audio")
 out
}
private suspend fun ensureModel(ctx:Context,onProgress:(Int)->Unit):File = withContext(Dispatchers.IO) {
 val dir=File(ctx.filesDir,"models").apply{mkdirs()}; val model=File(dir,"ggml-base.bin")
 if(model.exists() && model.length()>100_000_000L){onProgress(100);return@withContext model}
 val temp=File(dir,"ggml-base.bin.part"); var connection:HttpURLConnection?=null
 try {
  connection=(URL(MODEL_URL).openConnection() as HttpURLConnection).apply{connectTimeout=20000;readTimeout=60000;instanceFollowRedirects=true;setRequestProperty("User-Agent","SubN3xt-Studio/1.0")}
  connection.connect(); val code=connection.responseCode
  if(code !in 200..299) throw IllegalStateException("Model download failed (HTTP $code)")
  val total=connection.contentLengthLong
  connection.inputStream.use { input -> temp.outputStream().use { output ->
   val buffer=ByteArray(262144);var count=0L
   while(true){val n=input.read(buffer);if(n<0)break;output.write(buffer,0,n);count+=n;if(total>0)onProgress((count*100/total).toInt().coerceIn(0,99))}
  }}
  if(temp.length()<100_000_000L)throw IllegalStateException("Model download incomplete")
  if(model.exists())model.delete()
  if(!temp.renameTo(model))throw IllegalStateException("Could not save Whisper model")
  onProgress(100);model
 } catch(e:Exception){temp.delete();throw e} finally{connection?.disconnect()}
}
private suspend fun translateChineseToThai(text:String):String = suspendCoroutine { continuation ->
 val translator=Translation.getClient(TranslatorOptions.Builder()
  .setSourceLanguage(TranslateLanguage.CHINESE).setTargetLanguage(TranslateLanguage.THAI).build())
 translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
  .addOnSuccessListener { translator.translate(text).addOnSuccessListener { result ->
   translator.close(); continuation.resume(result)
  }.addOnFailureListener { error -> translator.close(); continuation.resumeWithException(error) } }
  .addOnFailureListener { error -> translator.close(); continuation.resumeWithException(error) }
}

@Composable fun SubN3xtApp(ctx:Context){
 var projects by remember{mutableStateOf(loadProjects(ctx))};var selected by remember{mutableStateOf<MediaProject?>(null)}
 var tab by remember{mutableStateOf("Home")};var cues by remember{mutableStateOf(listOf(Cue()))};var notice by remember{mutableStateOf("")}
 var language by remember{mutableStateOf("auto")};var translating by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var progress by remember{mutableIntStateOf(0)};var status by remember{mutableStateOf("")}
 val scope=rememberCoroutineScope()
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->if(uri!=null){
  try{ctx.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){}
  val p=MediaProject(System.currentTimeMillis(),displayName(ctx,uri),uri.toString());projects=projects+p;saveProjects(ctx,projects);selected=p;cues=listOf(Cue());tab="Editor";notice="Audio imported"
 }}
 val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")){uri:Uri?->if(uri!=null)try{
  val s=buildSrt(cues)
  ctx.contentResolver.openOutputStream(uri)?.use{it.write(s.toByteArray(Charsets.UTF_8))};notice="SRT exported"
 }catch(e:Exception){notice="Export error: ${e.message}"}}
 fun translateAll(){ if(cues.isEmpty()||translating)return; scope.launch {
  translating=true
  try { val translated=withContext(Dispatchers.IO){ cues.map { cue -> cue.copy(text=if(cue.text.isBlank()) "" else translateChineseToThai(cue.text)) } }
   cues=translated; notice="แปลภาษาจีนเป็นภาษาไทยเสร็จแล้ว"
  } catch(e:Exception){ notice="Translation failed: ${e.message?:e.javaClass.simpleName}" }
  finally { translating=false }
 }}
 fun importAudio(){picker.launch(arrayOf("audio/wav","audio/x-wav","audio/mpeg","audio/mp3","audio/flac"))}
 fun transcribe(){val project=selected?:return;scope.launch{
  busy=true;progress=0;status="Preparing audio…";var modelHandle:dev.ffmpegkit.whisper.WhisperModel?=null;var audioFile:File?=null
  try{
   val ext=project.name.substringAfterLast('.', "").lowercase()
   if(ext !in listOf("wav","mp3","flac"))throw IllegalArgumentException("Please select WAV, MP3 or FLAC audio.")
   audioFile=copyUriToCache(ctx,Uri.parse(project.uri));status="Downloading Whisper model…"
   val modelFile=ensureModel(ctx){progress=it}
   status="Loading Whisper model…";modelHandle=withContext(Dispatchers.IO){Whisper.loadModel(ctx,modelFile.absolutePath)}
   status="Transcribing on this device…"
   val result=withContext(Dispatchers.IO){Whisper.transcribe(modelHandle!!,audioFile.absolutePath,WhisperConfig(language=language))}
   val generated=result.segments.map{seg->Cue(msToSrt(seg.startMs),msToSrt(seg.endMs),seg.text.trim())}.filter{it.text.isNotBlank()}
   if(generated.isEmpty())notice="No speech detected. Try a clearer audio file." else{cues=generated;notice="Transcription complete: ${generated.size} lines"}
   status=""
  }catch(e:Exception){notice="Transcription failed: ${e.message?:e.javaClass.simpleName}";status=""}
  finally{modelHandle?.let{try{withContext(Dispatchers.IO){Whisper.releaseModel(it)}}catch(_:Exception){}};audioFile?.delete();busy=false;progress=0}
 }}
 MaterialTheme(colorScheme=darkColorScheme(background=Ink,surface=Panel,primary=Violet,secondary=Mint,onSurface=Color.White)){
  Scaffold(containerColor=Ink,bottomBar={NavigationBar(containerColor=Panel){listOf("Home","Projects","Editor","Settings").forEach{item->
   val icon=when(item){"Home"->Icons.Rounded.Home;"Projects"->Icons.Rounded.VideoLibrary;"Editor"->Icons.Rounded.Subtitles;else->Icons.Rounded.Settings}
   NavigationBarItem(selected=tab==item,onClick={tab=item},icon={Icon(icon,item)},label={Text(item,fontSize=10.sp)})
  }}}){pad->when(tab){
   "Home"->HomeScreen(Modifier.padding(pad),projects.size){importAudio()}
   "Projects"->ProjectsScreen(Modifier.padding(pad),projects,{p->selected=p;cues=listOf(Cue());tab="Editor"},{importAudio()},{p->projects=projects.filterNot{it.id==p.id};saveProjects(ctx,projects);if(selected?.id==p.id)selected=null})
   "Editor"->EditorScreen(Modifier.padding(pad),selected,cues,{cues=it},{importAudio()},{transcribe()},{exporter.launch((selected?.name?:"subtitles").substringBeforeLast('.')+".srt")},{translateAll()},translating,language,{language=it},busy,progress,status)
   else->Column(Modifier.padding(pad).fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.Settings,null,tint=Mint,modifier=Modifier.size(48.dp));Text("SubN3xt Studio",fontSize=24.sp);Text("Whisper runs locally after model download.",color=Color.LightGray)}
  }}
  if(notice.isNotBlank()){LaunchedEffect(notice){kotlinx.coroutines.delay(3200);notice=""};Surface(color=Color(0xFF26334D),shape=RoundedCornerShape(12.dp),modifier=Modifier.padding(12.dp)){Text(notice,color=Color.White,modifier=Modifier.padding(14.dp))}}
 }
}
private fun loadProjects(c:Context):List<MediaProject> = try{val a=JSONArray(c.getSharedPreferences("subn3xt",0).getString("projects","[]"));(0 until a.length()).map{val o=a.getJSONObject(it);MediaProject(o.getLong("id"),o.getString("name"),o.getString("uri"))}}catch(_:Exception){emptyList()}
private fun saveProjects(c:Context,ps:List<MediaProject>){val a=JSONArray();ps.forEach{a.put(JSONObject().put("id",it.id).put("name",it.name).put("uri",it.uri))};c.getSharedPreferences("subn3xt",0).edit().putString("projects",a.toString()).apply()}

@Composable fun HomeScreen(modifier:Modifier,count:Int,onImport:()->Unit){LazyColumn(modifier.fillMaxSize().padding(20.dp),contentPadding=PaddingValues(top=20.dp,bottom=24.dp)){item{
 Text("SUBN3XT",color=Mint,fontWeight=FontWeight.Bold,letterSpacing=3.sp);Text("Studio",fontSize=34.sp,fontWeight=FontWeight.ExtraBold);Text("On-device subtitle transcription",color=Color.LightGray)
 Spacer(Modifier.height(24.dp));Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF29285A),Color(0xFF172E45))),RoundedCornerShape(24.dp)).padding(22.dp)){
  Icon(Icons.Rounded.AutoAwesome,null,tint=Mint);Spacer(Modifier.height(12.dp));Text("Audio to subtitles",fontSize=25.sp,fontWeight=FontWeight.Bold);Text("Whisper AI • Thai & English • On-device",color=Color.LightGray)
  Spacer(Modifier.height(18.dp));Button(onClick=onImport,modifier=Modifier.fillMaxWidth()){Icon(Icons.Rounded.Add,null);Text("  Import audio")}
 }
 Spacer(Modifier.height(22.dp));Text("PROJECTS  $count",color=Mint,fontWeight=FontWeight.Bold);Spacer(Modifier.height(14.dp));Text("1  Import WAV, MP3 or FLAC");Text("2  Transcribe with Whisper");Text("3  Edit timestamps and export SRT")
}}}
@Composable fun ProjectsScreen(modifier:Modifier,ps:List<MediaProject>,open:(MediaProject)->Unit,add:()->Unit,delete:(MediaProject)->Unit){
 Column(modifier.fillMaxSize().padding(18.dp)){Text("Projects",fontSize=30.sp,fontWeight=FontWeight.Bold);Text("${ps.size} saved projects",color=Color.LightGray);Button(onClick=add,modifier=Modifier.fillMaxWidth().padding(vertical=12.dp)){Icon(Icons.Rounded.Add,null);Text(" Import audio")}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(ps,key={it.id}){p->Surface(color=Panel,shape=RoundedCornerShape(14.dp)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.AudioFile,null,tint=Mint);Spacer(Modifier.width(10.dp));Text(p.name,color=Color.White,modifier=Modifier.weight(1f));IconButton(onClick={open(p)}){Icon(Icons.Rounded.Edit,null,tint=Mint)};IconButton(onClick={delete(p)}){Icon(Icons.Rounded.DeleteOutline,null,tint=Color.Red)}}}}}
 }}
@Composable fun EditorScreen(modifier:Modifier,p:MediaProject?,cues:List<Cue>,change:(List<Cue>)->Unit,addMedia:()->Unit,transcribe:()->Unit,export:()->Unit,translate:()->Unit,translating:Boolean,language:String,setLanguage:(String)->Unit,busy:Boolean,progress:Int,status:String){
 Column(modifier.fillMaxSize().padding(16.dp)){Text("Subtitle Editor",fontSize=27.sp,fontWeight=FontWeight.Bold)
 if(p==null){Text("Select a project or import audio.",color=Color.LightGray);Button(onClick=addMedia){Text("Import audio")}}
 else{
  Text(p.name,color=Mint,maxLines=2);Text("Whisper input: WAV, MP3 or FLAC.",fontSize=12.sp,color=Color.LightGray)
  var expanded by remember{mutableStateOf(false)}
  Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
   Box(Modifier.weight(1f)){OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth(),enabled=!busy){Text("Language: ${language.uppercase()}")};DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){listOf("auto","th","en").forEach{code->DropdownMenuItem(text={Text(when(code){"auto"->"Auto detect";"th"->"Thai";else->"English"})},onClick={setLanguage(code);expanded=false})}}}
   Button(onClick=transcribe,modifier=Modifier.weight(1f),enabled=!busy){Icon(Icons.Rounded.AutoAwesome,null);Text(if(busy)" Working…" else " Transcribe")}
  }
  if(busy){Text(status,color=Mint,fontSize=12.sp,modifier=Modifier.padding(top=8.dp));if(status.contains("Downloading")){LinearProgressIndicator(progress={progress/100f},modifier=Modifier.fillMaxWidth().padding(top=5.dp));Text("Model download: $progress%",color=Mint,fontSize=11.sp)}else LinearProgressIndicator(modifier=Modifier.fillMaxWidth().padding(top=5.dp))}
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(vertical=10.dp)){
   OutlinedButton(onClick={change(cues+Cue())},modifier=Modifier.weight(1f),enabled=!busy){Icon(Icons.Rounded.Add,null);Text(" Add line")}
   Button(onClick=export,modifier=Modifier.weight(1f),enabled=!busy&&cues.isNotEmpty()){Icon(Icons.Rounded.FileDownload,null);Text(" Export SRT")}
  }
  OutlinedButton(onClick=translate,modifier=Modifier.fillMaxWidth(),enabled=!busy&&!translating&&cues.isNotEmpty()){Text(if(translating)"Translating Chinese → Thai…" else "Translate all: Chinese → Thai")}
  if(translating){LinearProgressIndicator(modifier=Modifier.fillMaxWidth().padding(bottom=8.dp));Text("Downloading translation model / translating…",color=Mint,fontSize=11.sp)}
  LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(cues.size){i->val c=cues[i];Surface(color=Panel,shape=RoundedCornerShape(14.dp)){Column(Modifier.fillMaxWidth().padding(12.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){Text("LINE ${i+1}",color=Mint,modifier=Modifier.weight(1f));IconButton(onClick={if(cues.size>1)change(cues.filterIndexed{j,_->j!=i})},enabled=!busy){Icon(Icons.Rounded.Close,null,tint=Color.Red)}}
   OutlinedTextField(c.start,{v->change(cues.mapIndexed{j,x->if(j==i)x.copy(start=v)else x})},label={Text("Start (HH:MM:SS,mmm)")},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),enabled=!busy)
   OutlinedTextField(c.end,{v->change(cues.mapIndexed{j,x->if(j==i)x.copy(end=v)else x})},label={Text("End (HH:MM:SS,mmm)")},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),enabled=!busy)
   OutlinedTextField(c.text,{v->change(cues.mapIndexed{j,x->if(j==i)x.copy(text=v)else x})},label={Text("Subtitle text")},modifier=Modifier.fillMaxWidth(),minLines=2,enabled=!busy)
  }}}}
 }
 }}
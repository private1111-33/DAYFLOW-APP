package com.dayflow.app.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dayflow.app.data.AlarmScheduler
import com.dayflow.app.data.AlarmStore
import com.dayflow.app.data.DayAlarm
import com.dayflow.app.service.AlarmRingService
import com.dayflow.app.ui.theme.*
import java.util.Locale

@Composable
fun AlarmsScreen() {
    val context=LocalContext.current
    val lifecycleOwner=LocalLifecycleOwner.current
    val alarms=remember { mutableStateListOf<DayAlarm>().apply { addAll(AlarmStore.all(context)) } }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DayAlarm?>(null) }
    var pendingDelete by remember { mutableStateOf<DayAlarm?>(null) }
    var permissionMissing by remember { mutableStateOf(!AlarmScheduler.canScheduleExact(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer=LifecycleEventObserver { _, event -> if(event==Lifecycle.Event.ON_RESUME) permissionMissing=!AlarmScheduler.canScheduleExact(context) }
        lifecycleOwner.lifecycle.addObserver(observer); onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun persist(){ AlarmStore.save(context,alarms.toList()) }
    ScreenColumn {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Alarm",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=TextPrimary)
                Text("Stay on track",color=TextSecondary)
            }
            FilledIconButton(onClick={editing=null;showDialog=true},shape=CircleShape,colors=IconButtonDefaults.filledIconButtonColors(containerColor=Burgundy)) { Icon(Icons.Rounded.Add,null,tint=Color.White) }
        }
        Box(Modifier.fillMaxWidth().height(170.dp),contentAlignment=Alignment.Center) {
            Box(Modifier.size(150.dp).clip(CircleShape).background(Burgundy.copy(alpha=.07f)))
            Icon(Icons.Rounded.Alarm, null, tint=Burgundy, modifier=Modifier.size(112.dp))
        }
        Text(if(alarms.isEmpty()) "No Alarms Yet" else "Your alarms",modifier=Modifier.fillMaxWidth(),textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=TextPrimary,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        Text(if(alarms.isEmpty()) "Add an alarm to stay on track" else "${alarms.count{it.enabled}} active reminders",modifier=Modifier.fillMaxWidth(),textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=TextSecondary)
        Button(onClick={editing=null;showDialog=true},modifier=Modifier.fillMaxWidth(.72f).height(54.dp).align(Alignment.CenterHorizontally),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Burgundy)) { Icon(Icons.Rounded.Add,null); Spacer(Modifier.width(8.dp)); Text("Add Alarm") }
        if(permissionMissing) DayflowCard {
            Text("Allow exact alarms",color=TextPrimary,fontWeight=FontWeight.Bold)
            Text("Android may delay alarms without this permission.",color=TextSecondary)
            Button(onClick={runCatching{context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:${context.packageName}")))}}) { Text("Grant permission") }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("Your Alarms",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=TextPrimary); Text("${alarms.size} saved",color=TextSecondary) }
        alarms.forEach { alarm ->
            DayflowCard(onClick={editing=alarm;showDialog=true}) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Burgundy.copy(alpha=.09f)),contentAlignment=Alignment.Center) { Icon(Icons.Rounded.Alarm, null, tint=Burgundy, modifier=Modifier.size(28.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(String.format(Locale.getDefault(),"%02d:%02d",alarm.hour,alarm.minute),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=TextPrimary)
                        Text(alarm.label.ifBlank{"Daily alarm"},color=TextSecondary)
                        Text(if(alarm.enabled) AlarmScheduler.daysSummary(alarm.days) else "Disabled",style=MaterialTheme.typography.labelSmall,color=Burgundy)
                    }
                    Switch(checked=alarm.enabled,onCheckedChange={enabled->AlarmScheduler.cancel(context,alarm);val updated=alarm.copy(enabled=enabled);val i=alarms.indexOfFirst{it.id==alarm.id};if(i>=0)alarms[i]=updated;persist();if(enabled)AlarmScheduler.schedule(context,updated)},colors=SwitchDefaults.colors(checkedThumbColor=Color.White,checkedTrackColor=Burgundy))
                    IconButton(onClick={pendingDelete=alarm}) { Icon(Icons.Rounded.DeleteOutline,"Delete",tint=TextSecondary) }
                }
            }
        }
    }
    if(showDialog) AlarmEditorDialog(initial=editing,onDismiss={showDialog=false},onSave={hour,minute,label,days->
        val old=editing
        if(old==null){val a=DayAlarm((System.currentTimeMillis()%Int.MAX_VALUE).toInt(),hour,minute,label,true,days);alarms.add(a);AlarmScheduler.schedule(context,a)}
        else{AlarmScheduler.cancel(context,old);val u=old.copy(hour=hour,minute=minute,label=label,days=days);val i=alarms.indexOfFirst{it.id==old.id};if(i>=0)alarms[i]=u;if(u.enabled)AlarmScheduler.schedule(context,u)}
        persist();showDialog=false
    })
    pendingDelete?.let{a->ConfirmDialog(title="Delete alarm?",message="${String.format(Locale.getDefault(),"%02d:%02d",a.hour,a.minute)} · ${a.label}",onConfirm={AlarmScheduler.cancel(context,a);alarms.removeAll{it.id==a.id};persist();pendingDelete=null},onDismiss={pendingDelete=null})}
}

@Composable
private fun AlarmEditorDialog(initial:DayAlarm?,onDismiss:()->Unit,onSave:(Int,Int,String,Int)->Unit){
    val context=LocalContext.current
    var label by remember{mutableStateOf(initial?.label.orEmpty())}
    var hour by remember{mutableIntStateOf(initial?.hour?:8)}
    var minute by remember{mutableIntStateOf(initial?.minute?:30)}
    var days by remember{mutableIntStateOf(initial?.days?:DayAlarm.ALL_DAYS)}
    val letters=listOf("S","M","T","W","T","F","S")
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(initial==null)"New alarm" else "Edit alarm")},text={
        Column(verticalArrangement=Arrangement.spacedBy(14.dp)){
            OutlinedTextField(value=label,onValueChange={label=it},label={Text("Alarm label")},singleLine=true)
            OutlinedButton(onClick={TimePickerDialog(context,{_,h,m->hour=h;minute=m},hour,minute,false).show()},modifier=Modifier.fillMaxWidth()){Text(String.format(Locale.getDefault(),"%02d:%02d",hour,minute),style=MaterialTheme.typography.titleLarge)}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){letters.forEachIndexed{i,l->val on=days and (1 shl i)!=0;Box(Modifier.size(32.dp).clip(CircleShape).background(if(on)Burgundy else SurfaceElevated).clickable{val n=days xor (1 shl i);if(n!=0)days=n},contentAlignment=Alignment.Center){Text(l,color=if(on)Color.White else TextSecondary,fontWeight=FontWeight.SemiBold)}}}
            Text(AlarmScheduler.daysSummary(days),color=TextSecondary)
        }
    },confirmButton={TextButton(onClick={onSave(hour,minute,label.trim().ifBlank{"Daily alarm"},days)}){Text("Save alarm",color=Burgundy)}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

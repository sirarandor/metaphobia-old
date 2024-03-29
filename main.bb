;Print("Metaphobia v0.0.5 - Deep Winter Studios")
.start
;Input("Press Enter to start.")
Graphics3D 1600,900,32,2 
HidePointer 
SetBuffer BackBuffer()
AmbientLight 8,8,8
Print("Loading...")


Include "player.bb"
Include "cell.bb"
Include "object.bb"

SeedRnd MilliSecs()

;GLOBAL VARIABLES

;PLAYER
Global player = CreatePivot()
ScaleEntity player,0.1,0.1,0.1
Global camera = CreateCamera(player)
Global flashlight = CreateLight(3,player)
Global selflight = CreateLight(2,player)
Global ears = CreateListener(player)

Global player_Height# = 3.2
Global player_Speed# = 0.08

Global sanity_max = 2400
Global battery_max = 1200

Global sanity = sanity_max
Global battery = battery_max

Global bob_dir = True
Global bob_amt# = 0
Global bob_add# = 0

Global flashlight_state = 0
Global fs_dead = 0
Global lost = 0

Global pcx%
Global pcy%
Global pmn,pme,pms,pmw

Global PLAY_COLL = 2


;SCALE
Const scale_x# = 2.7
Const scale_y# = 2.7
Const scale_z# = 0.1


;MAP
Const map_size_x = 1024
Const map_size_y = 1024

Const max_draw_x = 6
Const max_draw_y = 6

Global wall_density = 0
Global wall_weight = 250

;WATCHERS
;  These are sprite based so instead of giving them their own file I put them here to avoid annoyances. Meh. Sue me. 
Global watcher_max = 24 ; This number can be changed, allowing you to add as many watchers as you want! Fun! >:D
						; Yes, it can also massively slow the game down if you give it some truly silly numbers.
Global Entity Dim watchers(watcher_max)
Global EntityLight Dim watcherlight(watcher_max)

Function WatcherSetup()
	For i = 0 To watcher_max Step 1 
		watchers(i) = CreateSprite()
		watchers(i) = LoadSprite("Textures/watcher.png")
		watcherlight(i) = CreateLight(2,watchers(i))
		LightRange watcherlight(i),5
		LightColor watcherlight(i),100,100,150
		LightConeAngles watcherlight(i),15,45
		HideEntity(watcherlight(i))
		ScaleSprite(watchers(i), 2, 2)
		HideEntity(watchers(i)) 
	Next
End Function 



;DEBUG STUFF
Global CompassTex = LoadTexture("Textures/compass.bmp")

Global Cell Dim mainmap.Cell(map_size_x+1,map_size_y+1) ;Maze array


;CHUNK FUNCTIONS 

;Create a chunk and load it in
Function LoadChunk()
	
	minx = pcx-max_draw_x
	miny = pcy-max_draw_y

	maxx = pcx+max_draw_x
	maxy = pcy+max_draw_y

	For i = minx To maxx Step 1
		For j = miny To maxy Step 1
			If i > 0 And j > 0
				If mainmap(i,j) = Null
					r = Rnd(1,4)
					mainmap(i,j) = RndCell(i,j,r,wall_density)
					wall_weight = wall_weight - 1
					If wall_weight < 0  
						wall_density = Rnd(0,4)
						wall_weight = Rnd(1,200)
					EndIf
					
					UpdateWatchers() 
				EndIf
				ShowCell(mainmap(i,j))
			EndIf	
		Next
	Next
End Function


;Reload the chunk around the player, costing sanity
Function ReloadChunk()
	
	minx = pcx-max_draw_x
	miny = pcy-max_draw_y

	maxx = pcx+max_draw_x
	maxy = pcy+max_draw_y

	r = Rnd(3,4)
	For i = minx To maxx Step 1
		For j = miny To maxy Step 1
			If i > 0 And j > 0
				DeleteCell(mainmap(i,j))
				Delete(mainmap(i,j))
				mainmap(i,j) = RndCell(i,j,r,0)
				ShowCell(mainmap(i,j))
				sanity = sanity - Rnd(0.1,1)
			EndIf	
		Next
	Next   

End Function


;Delete the chunk around the player 
Function DeloadChunk()
	minx = pcx-max_draw_x
	miny = pcy-max_draw_y

	maxx = pcx+max_draw_x
	maxy = pcy+max_draw_y
	
	For i = 0 To map_size_x Step 1
		For j = 0 To map_size_y Step 1

			If i > 0 And j > 0
			If i < minx And mainmap(i,j) <> Null
			 	DeleteCell(mainmap(i,j))
				mainmap(i,j)\is = 0
				Delete(mainmap(i,j))
			EndIf
			If j < miny And mainmap(i,j) <> Null
			 	DeleteCell(mainmap(i,j))
				mainmap(i,j)\is = 0
				Delete(mainmap(i,j))
			EndIf
			If i > maxx And mainmap(i,j) <> Null
			 	DeleteCell(mainmap(i,j))
				mainmap(i,j)\is = 0
				Delete(mainmap(i,j))
			EndIf
			If j > maxy And mainmap(i,j) <> Null
			 	DeleteCell(mainmap(i,j))
				mainmap(i,j)\is = 0
				Delete(mainmap(i,j))
			EndIf
			EndIf
		Next
	Next

End Function


;Update the player cell position, what it says on the tin. Turns the player's absolute, global pixel position to an easier cell-based one so I can easily base where to load chunks.
Function UpdatePlayerCellPosition()
	ux% = Floor(EntityX(player) / 5.4)
	uy% = Floor(EntityZ(player) / 5.4)

	pcx = ux
	pcy = uy
End Function


; Update the watcher position, spawn them in if the player has less sanity. Spent a good 30 seconds searching for this before I realized I never commented above it like every other function. 
Function UpdateWatchers()
	x_min = pcx-max_draw_x
	z_min = pcy-max_draw_y

	x_max = pcx+max_draw_x
	z_max = pcy+max_draw_y

	rnd_roll = 0
	posx = 0.0
	posy = 0.0

	For i = 0 To watcher_max Step 1
		
		rnd_roll = Rnd(1,4)

		;Top Wall
		If rnd_roll = 1
			rnd_roll = Rnd(x_min,x_max)
			If mainmap(rnd_roll,z_min) <> Null
				posx = EntityX(mainmap(rnd_roll,z_min)\cf) ;+ Rnd(-50,50)
				posy = EntityZ(mainmap(rnd_roll,z_min)\cf) ;+ Rnd(-50,50)
				PositionEntity(watchers(i),posx,player_Height,posy)
			EndIf 
			rnd_roll = 0
		EndIf

		;Bottom Wall
		If rnd_roll = 2
			rnd_roll = Rnd(x_min,x_max)
			If mainmap(rnd_roll,z_max) <> Null
				posx = EntityX(mainmap(rnd_roll,z_max)\cf) ;+ Rnd(-50,50)
				posy = EntityZ(mainmap(rnd_roll,z_max)\cf) ;+ Rnd(-50,50)
				PositionEntity(watchers(i),posx,player_Height,posy)
			EndIf
			rnd_roll = 0
		EndIf 

		;Left Wall
		If rnd_roll = 3
			rnd_roll = Rnd(z_min, z_max)
			If mainmap(x_max,rnd_roll) <> Null
				posx = EntityX(mainmap(x_max,rnd_roll)\cf) ;+ Rnd(-50,50)
				posy = EntityZ(mainmap(x_max,rnd_roll)\cf) ;+ Rnd(-50,50)
				PositionEntity(watchers(i),posx,player_Height,posy)
			EndIf
			rnd_roll = 0
		EndIf

		;Right Wall
		If rnd_roll = 4
			rnd_roll = Rnd(z_min,z_max)
			If mainmap(x_min,rnd_roll) <> Null
				posx = EntityX(mainmap(x_min,rnd_roll)\cf) ;+ Rnd(-50,50)
				posy = EntityZ(mainmap(x_min,rnd_roll)\cf) ;+ Rnd(-50,50)
				PositionEntity(watchers(i),posx,player_Height,posy)
			EndIf
			rnd_roll = 0
		EndIf
	
		HideEntity(watchers(i))
		HideEntity(watcherlight(i))
		TranslateEntity(watchers(i),Rnd(-2.7,2.7),Rnd(2.7*3),Rnd(-2.7,2.7))
		rnd_roll = Rnd(0,sanity_max)
		If rnd_roll > sanity
			ShowEntity(watchers(i))
			ShowEntity(watcherlight(i))
		EndIf

	Next
End Function

Function ActiveWatchers()
	
	For i = 0 To watcher_max Step 1
		;TranslateEntity(watchers(i),Rnd(-1,1),Rnd(1,1),Rnd(1,1))
		
		If EntityInView(watchers(i),camera)
			
		EndIf 
	Next

End Function


;Status check, checks if the player has battery and sanity. And isn't dead.
Function StatusCheck()

	If flashlight_state = 1 
		battery = battery - Rnd(0,1)
		LightRange flashlight,(battery/25)
		If sanity < sanity_max Then sanity = sanity + 1
	EndIf

	If mainmap(pcx,pcy)\ntable = 1 Or mainmap(pcx+1,pcy)\ntable = 1 Or mainmap(pcx-1,pcy)\ntable = 1 Or mainmap(pcx,pcy+1)\ntable = 1 Or mainmap(pcx,pcy-1)\ntable = 1
		sanity = sanity + 1
	EndIf

	If flashlight_state = 0
		If mainmap(pcx,pcy)\ntable = 0 And mainmap(pcx+1,pcy)\ntable = 0 And mainmap(pcx-1,pcy)\ntable = 0 And mainmap(pcx,pcy+1)\ntable = 0 And mainmap(pcx,pcy-1)\ntable = 0 Then sanity = sanity - Rnd(1,3)
		If battery < battery_max Then battery = battery + Rnd(1,3)
	EndIf 


	p = 0

	If KeyHit(33)
		If flashlight_state = 0 And fs_dead = 0 And p = 0
		flashlight_state = 1
		LightRange flashlight,200
		LightRange selflight,20
		p = 1
	EndIf

	If flashlight_state = 1 Or battery = 0 And p = 0
		flashlight_state = 0
		LightRange flashlight,0
		LightRange selflight,0
		p = 1
	EndIf 
	EndIf


	If battery = 0 
		fs_dead = 1
		flashlight_state = 0
		LightRange flashlight,0
		LightRange selflight,0
	EndIf

	If battery = battery_max/2 Then fs_dead = 0
	If sanity <= 0 Then lost = 1

End Function


;Flashlight variables 
LightRange flashlight,0
LightRange selflight,0
LightConeAngles flashlight,30,45
LightColor flashlight,255,214,170
LightColor selflight,255,214,170


;Camera variables.
CameraProjMode camera,1   
CameraRange camera,1,max_draw_x*60

CameraFogMode camera,1
CameraFogRange camera,max_draw_x*40,max_draw_x*60


CameraFogColor camera,8,5,1
CameraClsColor camera,8,5,1

;Player collider. We do need this.
EntityType player,PLAY_COLL


;Debug compass, unlikely you will see this but I'm leaving it in so if you do you can get a cookie. 
Global compass = CreateCube()
RotateEntity compass,90,0,0
EntityTexture compass,CompassTex
ScaleEntity compass,2.7,2.7,0.1
PositionEntity(compass,-12,0,-12)


;Randomly put the player somewhere
px = Floor((map_size_x/2)*scale_x)
py = Floor((map_size_x/2)*scale_x)
PositionEntity player,px,player_Height,py

;Get the player's starting position
UpdatePlayerCellPosition()

;Add the watchers. This is a terrible implementation but I'm too lazy and doing this while actually working so whatever gets the file to compile will make me happy.
WatcherSetup()

;Actually load the starting chunks of the game
LoadChunk()
DeloadChunk()
Collisions(PLAY_COLL,WALL_COLL,2,2)
;Input("Loading completed.")

;Debug FPS stuff. I really should put this in the other debug category but am lazy and want to work on the Watchers. 
fpsTimer = 0 
fps = 0
fpsTicks = 0



;While not dead or scared, play the game.
While Not KeyHit(1) Or lost = 1	

	;Update the chunk around the player 
	UpdatePlayerCellPosition()
	LoadChunk()
	DeloadChunk()
	
	r = Rnd(500,1500)
	If MilliSecs() - fpsTimer > r Then StatusCheck()

	TurnCamera(camera,player,0.2)
	ControlPlayer(player)
	
	ActiveWatchers()
	

	If (MilliSecs() - fpsTimer > 1000)
		fpsTimer = MilliSecs()
		fps = fpsTicks
		fpsTicks = 0
	Else
		fpsTicks = fpsTicks + 1
	EndIf

	If KeyDown(19) Then ReloadChunk()

	UpdateWorld
	RenderWorld
		Text 6,6,fps
		Text 6,24, "Sanity: " + Floor(sanity / 10)
		Text 6,36, "Battery: " + Floor(battery / 10)
		Text 6,48, "Watcher [1] Location:" + Floor(EntityX(watchers(1))) + " " + Floor(EntityY(watchers(1))) + " " + Floor(EntityZ(watchers(1)))	
		Text 6,60, "Exact Player Location:" + EntityX(player) + " " + EntityY(player) + " " + EntityZ(player)
		Text 6,72, "Wall weight: " + wall_weight + " Wall density: " + wall_density 

		;Text 6,54,pcx
		;Text 6,66,pcy
		;Text 6,72,mainmap(pcx,pcy)\ntable
	Flip
Wend

End
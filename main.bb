Print("Metaphobia v0.0.5 - Deep Winter Studios")
.start
Input("Press Enter to start.")
Graphics3D 800,600,32,2
HidePointer 
SetBuffer BackBuffer()
AmbientLight 6,6,6
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
Global flashlight = CreateLight(2,player)
Global ears = CreateListener(player)

Global player_Height# = 3.2

Global sanity = 600
Global battery = 1200

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
Const map_size_x = 512
Const map_size_y = 512

Const max_draw_x = 4
Const max_draw_y = 4

;WATCHERS
;  These are sprite based so instead of giving them their own file I put them here to avoid annoyances. Meh. Sue me. 
Global watcher_max = 3
Global Entity Dim watchers(watcher_max)

Function WatcherSetup()
	For i = 0 To watcher_max Step 1 
		watchers(i) = CreateSprite()
		watchers(i) = LoadSprite("Textures/watcher.png")
		PositionEntity(watchers(i), EntityX(player), player_Height, EntityZ(player))
		ScaleSprite(watchers(i), scale_x, scale_y)
		ShowEntity(watchers(i))
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

	r = Rnd(1,2)

	For i = minx To maxx Step 1
		For j = miny To maxy Step 1
			If i > 0 And j > 0
				If mainmap(i,j) = Null Then mainmap(i,j) = RndCell(i,j,r)
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

	r = Rnd(1,4)
	For i = minx To maxx Step 1
		For j = miny To maxy Step 1
			If i > 0 And j > 0
				DeleteCell(mainmap(i,j))
				Delete(mainmap(i,j))
				mainmap(i,j) = RndCell(i,j,r)
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


;Controls the flashlight light. 
Function FlashlightControl()
	p = 0

	If KeyHit(33)
		If flashlight_state = 0 And fs_dead = 0 And p = 0
		flashlight_state = 1
		LightRange flashlight,24
		p = 1
	EndIf

	If flashlight_state = 1 Or battery = 0 And p = 0
		flashlight_state = 0
		LightRange flashlight,0
		p = 1
	EndIf
	EndIf 
End Function


;Status check, checks if the player has battery and sanity. And isn't dead.
Function StatusCheck()

	If flashlight_state = 1 
		battery = battery - Rnd(0,1)
		If sanity < 600 Then sanity = sanity + 1
	EndIf

	If mainmap(pcx,pcy)\ntable = 1
		sanity = sanity + 1
	EndIf

	If flashlight_state = 0
		If mainmap(pcx,pcy)\ntable = 0 Then sanity = sanity - Rnd(1,3)
		If battery < 1200 Then battery = battery + 1
	EndIf 

	If battery = 0 
		fs_dead = 1
		flashlight_state = 0
		LightRange flashlight,0
	EndIf

	If battery = 600 Then fs_dead = 0
	If sanity <= 0 Then lost = 1

End Function


;Flashlight variables 
LightRange flashlight,0
LightConeAngles flashlight,0,80
LightColor flashlight,255,183,76


;Camera variables. 
CameraFogMode camera,1
CameraFogRange camera,max_draw_x*36,max_draw_x*52

CameraFogColor camera,10,7,3
CameraClsColor camera,10,7,3


;Player collider. We do need this.
EntityType player,PLAY_COLL


;Debug compass, unlikely you will see this but I'm leaving it in so if you do you can get a cookie. 
Global compass = CreateCube()
RotateEntity compass,90,0,0
EntityTexture compass,CompassTex
ScaleEntity compass,2.7,2.7,0.1
PositionEntity(compass,-12,0,-12)


;Randomly put the player somewhere
px = Floor(Rnd(1381,3000)/5.4)
py = Floor(Rnd(1381,2764)/5.4)
PositionEntity player,px,player_Height,py

;Get the player's starting position
UpdatePlayerCellPosition()


;Actually load the starting chunks of the game
LoadChunk()
DeloadChunk()
Collisions(PLAY_COLL,WALL_COLL,2,2)
;Input("Loading completed.")

;Add the watchers. This is a terrible implementation but I'm too lazy and doing this while actually working so whatever gets the file to compile will make me happy.
WatcherSetup()

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
	FlashlightControl()

	TurnCamera(camera,player,0.2)
	ControlPlayer(player)

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
		Text 6,48, "Watcher [0] Location:" + Floor(EntityX(watchers(0))) + " " + Floor(EntityY(watchers(0))) + " " + Floor(EntityZ(watchers(0)))	
		Text 6,60, "Exact Player Location:" + EntityX(player) + " " + EntityY(player) + " " + EntityZ(player)



		;Text 6,54,pcx
		;Text 6,66,pcy
		;Text 6,72,mainmap(pcx,pcy)\ntable
	Flip

Wend

End
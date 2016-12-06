; merccc NSIS installer script
;--------------------------------

!include x64.nsh
!include FileAssociation.nsh

!define PRODUCT_NAME "Mercury Control Center (merccc)"
!define JRE_VERSION "1.8"
!define JRE_URL "http://download.oracle.com/otn-pub/java/jdk/8u101-b13/jre-8u101-windows-x64.exe"

Name "merccc"

OutFile "merccc-MERCCCVERSION.exe"

; The default installation directory
InstallDir $PROGRAMFILES\merccc

; Registry key to check for directory (so if you install again, it will 
; overwrite the old one automatically)
InstallDirRegKey HKLM "Software\merccc" "Install_Dir"

; Request application privileges for Windows Vista
RequestExecutionLevel admin


;--------------------------------

; Pages

Page components
Page directory
Page instfiles

UninstPage uninstConfirm
UninstPage instfiles

;--------------------------------

; The stuff to install
;Section "Download Java Runtime (if not already installed)"
;  Call DetectJRE
;SectionEnd

Section "Mercury Control Center Install (required)"
  Call DetectJREandFail
  SectionIn RO

  SetShellVarContext all
  
  ; Set output path to the installation directory.
  SetOutPath $INSTDIR
  
  ; Put file there
  File ".\merccc-MERCCCVERSION.jar"
  File ".\appicon.ico"
  File ".\appicon1.ico"

  IfFileExists $INSTDIR\merccc.bat 0 +2
    Delete $INSTDIR\merccc.bat    
  Push `for /f tokens^=2^ delims^=^" %%i in ('reg query HKEY_CLASSES_ROOT\jarfile\shell\open\command /ve') do set JAVAW_PATH=%%i$\r$\nif "%JAVAW_PATH%"=="" exit$\r$\nset JAVA_HOME=%JAVA_HOME:\javaw.exe=%$\r$\ncd /D $INSTDIR$\r$\n"%JAVAW_PATH%" -Djava.library.path=. -jar merccc-MERCCCVERSION.jar %*$\r$\n`
  Push `$INSTDIR\merccc.bat`
  Call WriteToFile
  
  IfFileExists $INSTDIR\merccco.bat 0 +2
    Delete $INSTDIR\merccco.bat    
  Push `cd /D $INSTDIR$\r$\nmerccc -c %*$\r$\n`
  Push `$INSTDIR\merccco.bat`
  Call WriteToFile
  
  IfFileExists $INSTDIR\mercccz.bat 0 +2
    Delete $INSTDIR\mercccz.bat    
  Push `cd /D $INSTDIR$\r$\nmerccc -z %*$\r$\n`
  Push `$INSTDIR\mercccz.bat`
  Call WriteToFile
  
  IfFileExists $INSTDIR\mercccserv.bat 0 +2
    Delete $INSTDIR\mercccserv.bat    
  Push `cd /D $INSTDIR$\r$\nmerccc -p 19000 %*$\r$\n`
  Push `$INSTDIR\mercccserv.bat`
  Call WriteToFile
  
  ; Write the installation path into the registry
  WriteRegStr HKLM SOFTWARE\merccc "Install_Dir" "$INSTDIR"
  
  ; Write the uninstall keys for Windows
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\merccc" "DisplayName" "Mercury Control Center Uninstallation"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\merccc" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\merccc" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\merccc" "NoRepair" 1
  WriteUninstaller "uninstall.exe"
  
SectionEnd

Section "Associate Mercury Configuration with the Program"
  ${unregisterExtension} ".merccc" "Mercury Control Center"
  ${unregisterExtension} ".merccz" "Mercury Control Center"
  ${registerExtension} "$INSTDIR\merccco.bat" ".merccc" "Mercury Control Center" "$INSTDIR\appicon.ico"
  ${registerExtension} "$INSTDIR\mercccz.bat" ".merccz" "Mercury Control Center" "$INSTDIR\appicon.ico"
SectionEnd

; Optional section (can be disabled by the user)
Section "Start Menu Shortcuts"
  CreateDirectory "$SMPROGRAMS\Mercury Control Center"
  CreateShortCut "$SMPROGRAMS\Mercury Control Center\Mercury Control Center.lnk" "$INSTDIR\merccc.bat" "" "$INSTDIR\appicon.ico" 0
  CreateShortCut "$SMPROGRAMS\Mercury Control Center\merccc with server (port 19000).lnk" "$INSTDIR\mercccserv.bat" "" "$INSTDIR\appicon1.ico" 0
  CreateShortCut "$SMPROGRAMS\Mercury Control Center\Uninstall.lnk" "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0   
  
SectionEnd

;--------------------------------

; Uninstaller

Section "Uninstall"
  SetShellVarContext all
  
  ; Remove registry keys
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\merccc"
  DeleteRegKey HKLM SOFTWARE\merccc

  ; Remove file association
  ${unregisterExtension} ".merccc" "Mercury Control Center configuration"
  
  ; Remove files and uninstaller
  Delete $INSTDIR\merccc-MERCCCVERSION.jar
  Delete $INSTDIR\merccc.bat
  Delete $INSTDIR\merccco.bat
  Delete $INSTDIR\mercccz.bat
  Delete $INSTDIR\mercccserv.bat
  Delete $INSTDIR\uninstall.exe
  Delete $INSTDIR\appicon.ico
  Delete $INSTDIR\appicon1.ico

  ; Remove shortcuts, if any
  Delete "$SMPROGRAMS\Mercury Control Center\*.*"

  ; Remove directories used
  RMDir "$SMPROGRAMS\Mercury Control Center"
  RMDir "$INSTDIR"

SectionEnd

Function DetectJREandFail

  ${If} ${RunningX64}
	SetRegView 64
  ${Else}
	SetRegView 32
  ${EndIf}

  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
    ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
    StrCpy $R0 "$R0\bin\java.exe"
    IfErrors error
    IfFileExists $R0 0 error
   
   Goto done
   
   error:
    MessageBox MB_ICONSTOP "Failed to detect a Java Runtime Environment! You can download Java Runtime from www.oracle.com"
    Abort   
   
   done:
FunctionEnd

Function WriteToFile
Exch $0 ;file to write to
Exch
Exch $1 ;text to write
 
  FileOpen $0 $0 a #open file
  FileSeek $0 0 END #go to end
  FileWrite $0 $1 #write to file
  FileClose $0
 
Pop $1
Pop $0
FunctionEnd
 
!macro WriteToFile NewLine File String
  !if `${NewLine}` == true
  Push `${String}$\r$\n`
  !else
  Push `${String}`
  !endif
  Push `${File}`
  Call WriteToFile
!macroend
!define WriteToFile `!insertmacro WriteToFile false`
!define WriteLineToFile `!insertmacro WriteToFile true`

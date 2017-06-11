#!/bin/bash

#execute once - display color gradient scale to pinpoint DPI issues
echo "display scale to check display"
fbi -T 1 -a /boot/PicLib/6291.png
sleep 10

while true
do
  echo "Check for new images, display them and move to cache."
  while true
  do
      if [ "$(ls -A /boot/Pictures)" ]; then
        echo "New file found"
        sleep 4
        for newfile in /boot/Pictures/*.*
        do
          echo "Display $newfile"
          fbi -T 1 -a $newfile
          sleep 30
          echo "move $newfile"
          mv $newfile /boot/PicCache
        done
      else
        echo "No more new files. Break" 
        break
      fi    
  done

  echo "Enter main loop: Vew pictures until a new one is sent"
  while true
  do
    echo "run through slideshow"
    for file in /boot/PicCache/*.*
    do
      fbi -T 1 -a $file
      for delay in {1..15}
      do
        sleep 1
        if [ "$(ls -A /boot/Pictures)" ]; then
          echo "New image. Abort main loop" 
          pkill fbi
          break 3
        fi
      done
      pkill fbi
      find /var/log/PF* -size 1M -delete
    done
  done
done

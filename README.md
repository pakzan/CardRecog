# CardRecog
This Android App recognises poker card and displays its Rank and Suit.

This Program requires Android OpenCV to be installed.  
Download and install \app\release\app-release.apk into your phone to test it.

Working Principle:
1. Greyscale and blur the image to remove unneccesary details.
2. Find the largest white area and identify it as a card. Thus, only the nearest poker card to the camera will be analysed.
3. Crop out the top left position of the card to obtain its Rank and Suit.
4. Compare the Rank and Suit with the pre-stored images and find the most similar one.
5. Display the results.  
  
  
A note for myself: 

Content in \app\build will change after "Clean Project" or "Rebuild Project".  
Remember to copy \app\libs\arm64-v8a\libapp.so and paste it into \app\build\intermediates\ndkBuild\release\obj\local\arm64-v8a after performing that operation.

# CardRecog
This Android App recognizes poker card and display its Rank and Suit.

This Program requires OpenCV to be installed.

Working Principle:
1. Greyscale and blur the image to remove unneccesary details.
2. Find the largest white area and identify it as a card. Thus, only the nearest poker card to the camera will be analysed.
3. Crop out the top left position of the card to obtain the Rank and Suit.
4. Compare the Rank and Suit with the prestored images and find the most similar one.
5. Display the results.

# LargeImageView
支持move、scale、fling、onDoubleTap
### Compile
```
allprojects {  
    	repositories {  
			...  
			maven { url "https://jitpack.io" }  
		}  
	}  
```
```
dependencies {  
	        compile 'com.github.Gcrj:LargeImageView:0.0.1'  
	}
```

### 使用方法
xml:  
```
<com.gcrj.largeimageviewlibrary.LargeImageView  
    android:id="@+id/liv"  
    android:layout_width="match_parent"  
    android:layout_height="match_parent"/>  
```

java:  
```
 LargeImageView liv = (LargeImageView) findViewById(R.id.liv);  
 try {  
        liv.setImage(getAssets().open("map.png"));//assets里的图片  
 } catch (IOException e) {  
        e.printStackTrace();  
 }
 ```
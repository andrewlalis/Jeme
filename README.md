# Jeme
Java-based meme maker application.

Run it quickly on linux with the included `jeme.sh` script. Run `./jeme.sh --help` for help information.

When you supply a path to the image to use to generate the meme, it'll also look for a file in the same directory, with the same name and a `.properties`. So if your image is in `data/img.png`, then it will look for `data/img.properties`. In that properties file, you can add a list of labels, each with a coordinate in floating-point format. For example:
```properties
top-text: 0.5, 0.15
bottom-text: 0.65, 0.82
```

Then you can call `./jeme.sh data/img.png -l top-text="Hello world" -l bottom-text="Bottom text"`.

If you're setting up an image, to use with Jeme, you can run `./jeme.sh <imageFilePath> --test-labels` to render the image with all labels shown. This can help with positioning the label centers.

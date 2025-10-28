# Upload Images with Go OpenAI Client v2

This guide demonstrates how to upload and analyze images using the `go-openai` client library (v2).

## Installation

```bash
go get github.com/sashabaranov/go-openai
```

## Setup

Set your OpenAI API key as an environment variable:

```bash
export OPENAI_API_KEY='your-api-key-here'
```

## Methods for Uploading Images

### 1. Image from URL

The simplest method - provide a direct URL to the image:

```go
req := openai.ChatCompletionRequest{
    Model: openai.GPT4VisionPreview,
    Messages: []openai.ChatCompletionMessage{
        {
            Role: openai.ChatMessageRoleUser,
            MultiContent: []openai.ChatMessagePart{
                {
                    Type: openai.ChatMessagePartTypeText,
                    Text: "What's in this image?",
                },
                {
                    Type: openai.ChatMessagePartTypeImageURL,
                    ImageURL: &openai.ChatMessageImageURL{
                        URL:    "https://example.com/image.jpg",
                        Detail: openai.ImageURLDetailAuto,
                    },
                },
            },
        },
    },
    MaxTokens: 300,
}
```

### 2. Image from Local File (Base64)

For local images, encode them as base64 and use a data URI:

```go
// Read and encode the image
imageData, _ := os.ReadFile("image.jpg")
base64Image := base64.StdEncoding.EncodeToString(imageData)

// Use in request
ImageURL: &openai.ChatMessageImageURL{
    URL:    fmt.Sprintf("data:image/jpeg;base64,%s", base64Image),
    Detail: openai.ImageURLDetailHigh,
}
```

### 3. Multiple Images

You can include multiple images in a single request:

```go
MultiContent: []openai.ChatMessagePart{
    {
        Type: openai.ChatMessagePartTypeText,
        Text: "Compare these images:",
    },
    {
        Type: openai.ChatMessagePartTypeImageURL,
        ImageURL: &openai.ChatMessageImageURL{
            URL: "https://example.com/image1.jpg",
        },
    },
    {
        Type: openai.ChatMessagePartTypeImageURL,
        ImageURL: &openai.ChatMessageImageURL{
            URL: "https://example.com/image2.jpg",
        },
    },
}
```

## Image Detail Levels

Control the analysis detail level:

- `openai.ImageURLDetailLow` - Faster, cheaper, less detailed
- `openai.ImageURLDetailHigh` - Slower, more expensive, more detailed
- `openai.ImageURLDetailAuto` - Let OpenAI decide (default)

## Supported Models

- `openai.GPT4VisionPreview` - GPT-4 Vision (legacy)
- `openai.GPT4Turbo` - GPT-4 Turbo with vision capabilities
- `openai.GPT4o` - Latest GPT-4o model with vision

## Supported Image Formats

- PNG
- JPEG
- WebP
- Non-animated GIF

## Size Limits

- URL images: No explicit limit, but consider API timeouts
- Base64 images: Maximum 20MB per image
- Multiple images: Up to 10 images per request

## Running the Example

```bash
# Set your API key
export OPENAI_API_KEY='sk-...'

# Run the example
go run upload_image_example.go
```

## Common Use Cases

1. **Image Analysis**: Describe what's in an image
2. **OCR**: Extract text from images
3. **Image Comparison**: Compare multiple images
4. **Object Detection**: Identify objects in images
5. **Scene Understanding**: Understand context and relationships

## Error Handling

Always handle errors properly:

```go
resp, err := client.CreateChatCompletion(ctx, req)
if err != nil {
    log.Fatalf("Error: %v", err)
}
```

## Best Practices

1. Use appropriate detail levels to balance cost and quality
2. Resize large images before encoding to reduce data transfer
3. Use URLs when possible to avoid base64 encoding overhead
4. Set reasonable `MaxTokens` limits
5. Implement proper error handling and retries
6. Cache responses when appropriate

## Additional Resources

- [Official go-openai repository](https://github.com/sashabaranov/go-openai)
- [OpenAI Vision API documentation](https://platform.openai.com/docs/guides/vision)
- [OpenAI API Reference](https://platform.openai.com/docs/api-reference)

package main

import (
	"context"
	"encoding/base64"
	"fmt"
	"io"
	"log"
	"os"

	"github.com/sashabaranov/go-openai"
)

func main() {
	apiKey := os.Getenv("OPENAI_API_KEY")
	if apiKey == "" {
		log.Fatal("OPENAI_API_KEY environment variable is required")
	}

	client := openai.NewClient(apiKey)

	// Example 1: Upload image from URL
	err := uploadImageFromURL(client)
	if err != nil {
		log.Printf("Error uploading from URL: %v", err)
	}

	// Example 2: Upload image from local file
	err = uploadImageFromFile(client, "example.jpg")
	if err != nil {
		log.Printf("Error uploading from file: %v", err)
	}

	// Example 3: Upload image as base64
	err = uploadImageAsBase64(client, "example.jpg")
	if err != nil {
		log.Printf("Error uploading as base64: %v", err)
	}
}

// uploadImageFromURL demonstrates uploading an image via URL
func uploadImageFromURL(client *openai.Client) error {
	ctx := context.Background()

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

	resp, err := client.CreateChatCompletion(ctx, req)
	if err != nil {
		return fmt.Errorf("chat completion error: %w", err)
	}

	fmt.Printf("Response from URL image: %s\n", resp.Choices[0].Message.Content)
	return nil
}

// uploadImageFromFile demonstrates uploading a local image file
func uploadImageFromFile(client *openai.Client, filePath string) error {
	ctx := context.Background()

	// Read the image file
	imageData, err := os.ReadFile(filePath)
	if err != nil {
		return fmt.Errorf("failed to read image file: %w", err)
	}

	// Encode to base64
	base64Image := base64.StdEncoding.EncodeToString(imageData)

	// Determine the media type based on file extension
	mediaType := "image/jpeg" // Adjust based on your file type

	req := openai.ChatCompletionRequest{
		Model: openai.GPT4VisionPreview,
		Messages: []openai.ChatCompletionMessage{
			{
				Role: openai.ChatMessageRoleUser,
				MultiContent: []openai.ChatMessagePart{
					{
						Type: openai.ChatMessagePartTypeText,
						Text: "Describe this image in detail.",
					},
					{
						Type: openai.ChatMessagePartTypeImageURL,
						ImageURL: &openai.ChatMessageImageURL{
							URL:    fmt.Sprintf("data:%s;base64,%s", mediaType, base64Image),
							Detail: openai.ImageURLDetailHigh,
						},
					},
				},
			},
		},
		MaxTokens: 500,
	}

	resp, err := client.CreateChatCompletion(ctx, req)
	if err != nil {
		return fmt.Errorf("chat completion error: %w", err)
	}

	fmt.Printf("Response from file image: %s\n", resp.Choices[0].Message.Content)
	return nil
}

// uploadImageAsBase64 demonstrates uploading an image as base64 data
func uploadImageAsBase64(client *openai.Client, filePath string) error {
	ctx := context.Background()

	file, err := os.Open(filePath)
	if err != nil {
		return fmt.Errorf("failed to open file: %w", err)
	}
	defer file.Close()

	imageData, err := io.ReadAll(file)
	if err != nil {
		return fmt.Errorf("failed to read file: %w", err)
	}

	base64Image := base64.StdEncoding.EncodeToString(imageData)

	req := openai.ChatCompletionRequest{
		Model: openai.GPT4Turbo, // or openai.GPT4VisionPreview
		Messages: []openai.ChatCompletionMessage{
			{
				Role: openai.ChatMessageRoleUser,
				MultiContent: []openai.ChatMessagePart{
					{
						Type: openai.ChatMessagePartTypeText,
						Text: "What objects can you identify in this image?",
					},
					{
						Type: openai.ChatMessagePartTypeImageURL,
						ImageURL: &openai.ChatMessageImageURL{
							URL:    "data:image/jpeg;base64," + base64Image,
							Detail: openai.ImageURLDetailAuto,
						},
					},
				},
			},
		},
		MaxTokens: 300,
	}

	resp, err := client.CreateChatCompletion(ctx, req)
	if err != nil {
		return fmt.Errorf("chat completion error: %w", err)
	}

	fmt.Printf("Response from base64 image: %s\n", resp.Choices[0].Message.Content)
	return nil
}

// Example for multiple images in a single request
func uploadMultipleImages(client *openai.Client) error {
	ctx := context.Background()

	req := openai.ChatCompletionRequest{
		Model: openai.GPT4VisionPreview,
		Messages: []openai.ChatCompletionMessage{
			{
				Role: openai.ChatMessageRoleUser,
				MultiContent: []openai.ChatMessagePart{
					{
						Type: openai.ChatMessagePartTypeText,
						Text: "Compare these two images:",
					},
					{
						Type: openai.ChatMessagePartTypeImageURL,
						ImageURL: &openai.ChatMessageImageURL{
							URL:    "https://example.com/image1.jpg",
							Detail: openai.ImageURLDetailAuto,
						},
					},
					{
						Type: openai.ChatMessagePartTypeImageURL,
						ImageURL: &openai.ChatMessageImageURL{
							URL:    "https://example.com/image2.jpg",
							Detail: openai.ImageURLDetailAuto,
						},
					},
				},
			},
		},
		MaxTokens: 500,
	}

	resp, err := client.CreateChatCompletion(ctx, req)
	if err != nil {
		return fmt.Errorf("chat completion error: %w", err)
	}

	fmt.Printf("Response: %s\n", resp.Choices[0].Message.Content)
	return nil
}

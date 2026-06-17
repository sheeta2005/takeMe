export function compressImage(file: File, maxWidth = 800, quality = 0.8): Promise<File> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()

    reader.onload = (e) => {
      const img = new Image()
      img.src = e.target?.result as string

      img.onload = () => {
        const canvas = document.createElement('canvas')
        let width = img.width
        let height = img.height

        if (width > maxWidth) {
          height = (height * maxWidth) / width
          width = maxWidth
        }

        canvas.width = width
        canvas.height = height

        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('Canvas context not available'))
          return
        }

        ctx.drawImage(img, 0, 0, width, height)

        canvas.toBlob(
          (blob) => {
            if (!blob) {
              reject(new Error('Compression failed'))
              return
            }

            const compressedFile = new File([blob], file.name, {
              type: file.type,
              lastModified: Date.now()
            })

            resolve(compressedFile)
          },
          file.type,
          quality
        )
      }

      img.onerror = () => {
        reject(new Error('Image load failed'))
      }
    }

    reader.onerror = () => {
      reject(new Error('File read failed'))
    }

    reader.readAsDataURL(file)
  })
}

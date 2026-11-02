package com.aldogor.stilme_qe_app.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateCheckerTest {

    // Drive's "Copy link" gives a viewer page, which returns HTML rather than the file
    @Test
    fun `a Drive viewer link becomes the direct download link of the same file`() {
        assertEquals(
            "https://drive.google.com/uc?export=download&id=INVENTED_file-id_0123456789",
            UpdateChecker.toDirectDownloadUrl(
                "https://drive.google.com/file/d/INVENTED_file-id_0123456789/view?usp=drive_link"
            )
        )
    }

    @Test
    fun `a direct download link is left as it is`() {
        val url = "https://drive.google.com/uc?export=download&id=INVENTED_file-id_0123456789"
        assertEquals(url, UpdateChecker.toDirectDownloadUrl(url))
    }

    @Test
    fun `a link outside Google Drive is left as it is`() {
        val url = "https://example.org/file/d/update-info.json"
        assertEquals(url, UpdateChecker.toDirectDownloadUrl(url))
    }
}

import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faList, faPencil, faPaperclip, faLightbulb, faCheckCircle, faTimesCircle, faUpload, faFile } from "@fortawesome/free-solid-svg-icons";
import { createTicket, getIssueCategories } from "../services/ticketApi";
import { useQueryClient } from "@tanstack/react-query";
import { uploadAttachment } from "../services/ticketApi";
import { getCurrentUser } from "../auth/auth";
import { useToast } from "../components/ToastProvider";
import { useForm, Controller } from "react-hook-form";
import { yupResolver } from "@hookform/resolvers/yup";
import * as yup from "yup";

const schema = yup.object({
  issueCategoryId: yup
    .number()
    .nullable()
    .transform((value, original) => (original === "" ? null : value)),
  description: yup.string().required("Description is required").min(10, "Description must be at least 10 characters"),
});

export default function CreateTicket() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const queryClient = useQueryClient();
  const [categories, setCategories] = useState([]);
  const [suggestedCategoryId, setSuggestedCategoryId] = useState(null);
  const [files, setFiles] = useState([]);

  const {
    control,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: yupResolver(schema), defaultValues: { issueCategoryId: "", description: "" } });

  const descriptionValue = watch("description");

  useEffect(() => {
    getIssueCategories().then((res) => setCategories(res));
  }, []);

  useEffect(() => {
    if (!categories || categories.length === 0) return;
    if (!descriptionValue || descriptionValue.length < 6) {
      setSuggestedCategoryId(null);
      return;
    }
    const suggested = autoCategorize(descriptionValue, categories);
    setSuggestedCategoryId(suggested?.categoryId || null);
  }, [descriptionValue, categories]);

  function extractTokens(text) {
    return (text || "")
      .toLowerCase()
      .replace(/[^a-z0-9\s]/g, " ")
      .split(/\s+/)
      .filter((w) => w.length > 2);
  }

  function autoCategorize(text, categoriesList) {
    const tokens = extractTokens(text);
    if (tokens.length === 0) return null;
    let best = null;
    let bestScore = 0;
    categoriesList.forEach((cat) => {
      const pool = (cat.categoryName || "") + " " + (cat.description || "");
      const poolTokens = extractTokens(pool);
      let score = 0;
      tokens.forEach((t) => {
        if (poolTokens.includes(t)) score += 2;
        // partial matches
        poolTokens.forEach((p) => {
          if (p.includes(t) || t.includes(p)) score += 0.5;
        });
      });
      if (score > bestScore) {
        bestScore = score;
        best = cat;
      }
    });
    // require at least a minimal score
    return bestScore >= 1 ? best : null;
  }

  async function onSubmit(values) {
    const user = getCurrentUser();
    // allow empty category: prefer explicit selection, else suggestedCategoryId, else null (server will auto-categorize)
    const explicitCat = values.issueCategoryId && values.issueCategoryId !== "" ? Number(values.issueCategoryId) : null;
    const finalCategoryId = explicitCat || suggestedCategoryId || null;

    const payload = {
      customerId: user.userId,
      issueCategoryId: finalCategoryId,
      description: values.description,
    };
    console.log('create payload:', payload);
    try {
      const ticket = await createTicket(payload);
      const ref = ticket?.ticketReference || ticket?.ticketId || 'created';
      showToast(`Ticket ${ref} created`, { severity: "success" });
      // upload any selected files
      if (files && files.length > 0 && ticket && ticket.ticketId) {
        for (let i = 0; i < files.length; i++) {
          try { await uploadAttachment(ticket.ticketId, files[i]); } catch (e) { console.error('attachment upload failed', e); }
        }
      }
      // refresh ticket list so new ticket is visible on /tickets
      try { queryClient.invalidateQueries(["tickets"]); } catch (e) { /* ignore */ }
      navigate("/tickets");
    } catch (err) {
      console.error(err);
      console.error('server response:', err?.response?.data);
      showToast(`Failed to create ticket: ${err?.response?.data || err.message}`, { severity: "error" });
    }
  }

  return (
    <div className="create-ticket-container">
      <div className="create-ticket-form-wrapper">
        <form onSubmit={handleSubmit(onSubmit)} className="create-ticket-form">
          
          {/* Category Section */}
          <div className="form-section">
            <h3 className="form-section-title"><FontAwesomeIcon icon={faList} style={{ marginRight: '0.5em' }} />Issue Category</h3>
            
            <Controller
              name="issueCategoryId"
              control={control}
              render={({ field }) => (
                <div className="form-group">
                  <label htmlFor="category" className="form-label">Select a category</label>
                  <select 
                    id="category"
                    {...field}
                    className={`form-select-custom ${errors.issueCategoryId ? 'is-invalid' : ''}`}
                  >
                    <option value="">-- Choose Issue Category --</option>
                    {categories.map((c) => (
                      <option key={c.categoryId} value={c.categoryId}>
                        {c.categoryName}
                      </option>
                    ))}
                  </select>
                  {errors.issueCategoryId && (
                    <div className="error-feedback">
                      {errors.issueCategoryId?.message}
                    </div>
                  )}
                </div>
              )}
            />

            {suggestedCategoryId && (
              <div className="suggestion-card">
                <div className="suggestion-header">
                  <FontAwesomeIcon icon={faLightbulb} className="suggestion-icon" />
                  <span className="suggestion-label">AI Suggestion</span>
                </div>
                <p className="suggestion-category">
                  {categories.find(c => c.categoryId === suggestedCategoryId)?.categoryName}
                </p>
                <div className="suggestion-actions">
                  <button 
                    type="button" 
                    className="btn-suggestion btn-accept" 
                    onClick={() => setValue('issueCategoryId', suggestedCategoryId)}
                  >
                    <FontAwesomeIcon icon={faCheckCircle} style={{ marginRight: '0.5em' }} />
                    Use Suggestion
                  </button>
                  <button 
                    type="button" 
                    className="btn-suggestion btn-dismiss" 
                    onClick={() => setSuggestedCategoryId(null)}
                  >
                    <FontAwesomeIcon icon={faTimesCircle} style={{ marginRight: '0.5em' }} />
                    Dismiss
                  </button>
                </div>
              </div>
            )}

            <p className="form-hint">
              <FontAwesomeIcon icon={faLightbulb} style={{ marginRight: '0.5em' }} />
              Not sure? The system will auto-categorize based on your description
            </p>
          </div>

          {/* Description Section */}
          <div className="form-section">
            <h3 className="form-section-title"><FontAwesomeIcon icon={faPencil} style={{ marginRight: '0.5em' }} />Problem Description</h3>
            
            <Controller
              name="description"
              control={control}
              render={({ field }) => (
                <div className="form-group">
                  <label htmlFor="description" className="form-label">Tell us about the issue</label>
                  <textarea 
                    id="description"
                    {...field}
                    className={`form-textarea-custom ${errors.description ? 'is-invalid' : ''}`}
                    rows="5"
                    placeholder="Please provide detailed information about your issue. Include any error messages, steps to reproduce, and what you were trying to do..."
                  />
                  <div className="textarea-info">
                    <span className="char-count">{field.value?.length || 0}/500</span>
                  </div>
                  {errors.description && (
                    <div className="error-feedback">
                      {errors.description?.message}
                    </div>
                  )}
                </div>
              )}
            />
          </div>

          {/* Attachments Section */}
          <div className="form-section">
            <h3 className="form-section-title"><FontAwesomeIcon icon={faPaperclip} style={{ marginRight: '0.5em' }} />Attachments</h3>
            
            <div className="file-upload-area">
              <input 
                type="file" 
                id="file-input"
                multiple 
                className="file-input-hidden"
                onChange={(e) => setFiles(Array.from(e.target.files))} 
              />
              <label htmlFor="file-input" className="file-upload-label">
                <div className="file-upload-content">
                  <FontAwesomeIcon icon={faUpload} className="file-icon" />
                  <p className="file-text">
                    Drag files here or <span className="file-browse">click to browse</span>
                  </p>
                  <small className="file-hint">PNG, JPG, PDF, TXT up to 10MB each</small>
                </div>
              </label>
              
              {files.length > 0 && (
                <div className="file-list">
                  <h4 className="file-list-title">Selected Files:</h4>
                  {files.map((f, i) => (
                    <div key={`file-${f.name}-${f.size}-${i}`} className="file-item">
                      <FontAwesomeIcon icon={faFile} className="file-item-icon" />
                      <div className="file-item-info">
                        <span className="file-item-name">{f.name}</span>
                        <span className="file-item-size">({(f.size / 1024).toFixed(2)} KB)</span>
                      </div>
                      <button 
                        type="button"
                        className="file-item-remove"
                        onClick={() => setFiles(files.filter((_, idx) => idx !== i))}
                        title="Remove file"
                      >
                        <FontAwesomeIcon icon={faTimesCircle} />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Action Buttons */}
          <div className="form-actions">
            <button 
              type="submit" 
              className="btn-submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <span className="spinner"></span>
                  Submitting...
                </>
              ) : (
                <>
                  ✓ Create Ticket
                </>
              )}
            </button>
            <button 
              type="button" 
              className="btn-cancel"
              onClick={() => navigate('/tickets')}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

